package org.umg.sistemamedicoii.service.gestion_citas_recepcion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.cache.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.agenda_medica_tareas.EventoAgenda;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.TipoCita;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;
import org.umg.sistemamedicoii.repository.agenda_medica_tareas.EventoAgendaRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.TipoCitaRepository;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.UsuarioRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;
import org.umg.sistemamedicoii.service.gestion_citas_recepcion.CitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CitaServiceImpl implements CitaService {
    private static final String ESTADO_CANCELADA = "Cancelada";
    private static final String ESTADO_PENDIENTE_PAGO = "Pendiente de pago";
    private static final String ESTADO_NO_ASISTIO = "No Asistió";
    private static final List<String> ESTADOS_EXCLUIDOS_CALENDARIO =
            List.of(ESTADO_CANCELADA, ESTADO_PENDIENTE_PAGO, ESTADO_NO_ASISTIO);
    private static final LocalTime HORA_INICIO = LocalTime.of(8,0);
    private static final LocalTime HORA_FIN = LocalTime.of(17,0);
    private static final int DURACION_MINUTOS = 30;
    private static final int MINUTOS_RESERVA = 5;

    @Autowired private EmailService emailService;
    @Autowired private TipoCitaRepository tipoCitaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private EstadoCitaCache estadoCache;
    @Autowired private EventoAgendaRepository eventoAgendaRepository;

    @Override
    public List<MedicoDisponibleResponseDTO> listarMedicosDisponibles(Integer sucursalId, Integer especialidadId){
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isActivo()
                        && u.getRol() != null
                        && ("Médico".equalsIgnoreCase(u.getRol().getNombre()) || "Medico".equalsIgnoreCase(u.getRol().getNombre()))
                        && sucursalId.equals(u.getSucursal() != null ? u.getSucursal().getId() : null)
                        && especialidadId.equals(u.getEspecialidad() != null ? u.getEspecialidad().getId() : null))
                .map(u -> {
                    MedicoDisponibleResponseDTO dto = new MedicoDisponibleResponseDTO();
                    dto.setId(u.getId());
                    dto.setNombreCompleto(u.getNombreCompleto());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<LocalDateTime> listarHorariosDisponibles(Integer medicoId, LocalDate fecha) {
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime cursor = fecha.atTime(HORA_INICIO);
        LocalDateTime fin = fecha.atTime(HORA_FIN);

        while (cursor.isBefore(fin)){
            slots.add(cursor);
            cursor = cursor.plusMinutes(DURACION_MINUTOS);
        }

        List<Cita> ocupadas = citaRepository.findByMedicoIdAndFechaHoraBetweenAndEstado_NombreNot(
                medicoId, fecha.atTime(HORA_INICIO), fin, ESTADO_CANCELADA);
        List<LocalDateTime> ocupadosList = ocupadas.stream().map(Cita::getFechaHora).collect(Collectors.toList());

        // GAP QA: además de las citas ya agendadas, un bloqueo de agenda del médico
        // (Bloqueo de disponibilidad, Evento personal, Capacitación, Vacaciones)
        // también debe quitar el horario de la lista de disponibles.
        List<EventoAgenda> bloqueos = eventoAgendaRepository
                .findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(medicoId, fin, fecha.atTime(HORA_INICIO));

        return slots.stream()
                .filter(slot -> !ocupadosList.contains(slot))
                .filter(slot -> bloqueos.stream().noneMatch(b -> seSuperpone(slot, slot.plusMinutes(DURACION_MINUTOS), b)))
                .collect(Collectors.toList());
    }

    private boolean seSuperpone(LocalDateTime inicioSlot, LocalDateTime finSlot, EventoAgenda bloqueo) {
        return inicioSlot.isBefore(bloqueo.getFechaFin()) && bloqueo.getFechaInicio().isBefore(finSlot);
    }

    @Override
    public CitaResponseDTO agendarCita(CitaRequestDTO dto, boolean creadaPorPersonalInterno) {
        if (!dto.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }

        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                dto.getMedicoId(), dto.getFechaHora(), ESTADO_CANCELADA)){
            throw new IllegalArgumentException("El horario seleccionado ya no esta disponible. Por favor, elija otro horario.");
        }

        // GAP QA: validación de respaldo en backend — aunque el horario ya no aparezca
        // en el selector, no debe poderse confirmar una cita dentro de un bloqueo de agenda.
        LocalDateTime finCita = dto.getFechaHora().plusMinutes(DURACION_MINUTOS);
        boolean horarioBloqueado = eventoAgendaRepository
                .findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(dto.getMedicoId(), finCita, dto.getFechaHora())
                .stream()
                .anyMatch(b -> seSuperpone(dto.getFechaHora(), finCita, b));
        if (horarioBloqueado) {
            throw new IllegalArgumentException("El horario seleccionado no está disponible porque el médico tiene un bloqueo de agenda. Por favor, elija otro horario.");
        }

        // RN-CU11-01 Validación de seguimiento
        if (dto.getCitaPadreId() != null) {
            if (dto.getTipoSeguimiento() == null || dto.getTipoSeguimiento().isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar el tipo de seguimiento.");
            }
            // Validación estricta de las opciones permitidas
            String tipo = dto.getTipoSeguimiento().trim();
            if (!tipo.equalsIgnoreCase("Monitoreo de Tratamiento") && !tipo.equalsIgnoreCase("Revisión de Resultados de Laboratorio")) {
                throw new IllegalArgumentException("Tipo de seguimiento inválido. Opciones: 'Monitoreo de Tratamiento' o 'Revisión de Resultados de Laboratorio'.");
            }
            // Solución CU-12 (gap del QA): RN-CU11-03 exige motivo del seguimiento Y prioridad
            if (dto.getPrioridadSeguimiento() == null || dto.getPrioridadSeguimiento().isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar la prioridad del seguimiento.");
            }
            String prioridad = dto.getPrioridadSeguimiento().trim();
            if (!prioridad.equalsIgnoreCase("Alta") && !prioridad.equalsIgnoreCase("Media") && !prioridad.equalsIgnoreCase("Baja")) {
                throw new IllegalArgumentException("Prioridad de seguimiento inválida. Opciones: 'Alta', 'Media' o 'Baja'.");
            }
        }

        Usuario paciente = usuarioRepository.findById(dto.getPacienteId())
                .orElseThrow(()-> new ResourceNotFoundException("Paciente no encontrado."));
        Usuario medico = usuarioRepository.findById(dto.getMedicoId())
                .orElseThrow(()-> new ResourceNotFoundException("Médico no encontrado."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(()-> new ResourceNotFoundException("Sucursal no encontrada."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(()-> new ResourceNotFoundException("Especialidad no encontrada."));
        TipoCita tipoCita = tipoCitaRepository.findById(dto.getTipoCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cita no encontrado."));

        EstadoCita estadoPendiente = estadoCache.getEstado(org.umg.sistemamedicoii.enums.EstadoCitaEnum.PENDIENTE_PAGO);

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setEstado(estadoPendiente);
        cita.setFechaHora(dto.getFechaHora());
        cita.setMotivo(dto.getMotivo());
        cita.setTipoCita(tipoCita);
        cita.setPrecio(tipoCita.getPrecio());
        // RN-CU03/CU-04: el temporizador de reserva de 5 minutos solo aplica al flujo de
        // autoagendamiento en línea del paciente (que muestra el ReservationTimer en pantalla).
        // Las citas creadas por personal interno (walk-in de CU-05, seguimiento de CU-11) no
        // tienen ese conteo activo frente al paciente; si se les fijaba igual un vencimiento de
        // 5 minutos, cuando el paciente intentaba pagar más tarde (CU-04) el sistema cancelaba
        // la cita por "tiempo expirado" antes de que pudiera completar el pago.
        cita.setReservadaHasta(creadaPorPersonalInterno ? null : LocalDateTime.now().plusMinutes(MINUTOS_RESERVA));
        cita.setFechaCreacion(LocalDateTime.now());
        cita.setCreadaPorPersonalInterno(creadaPorPersonalInterno);

        // Asignación de datos de seguimiento
        cita.setCitaPadreId(dto.getCitaPadreId());
        cita.setTipoSeguimiento(dto.getTipoSeguimiento());
        cita.setPrioridadSeguimiento(dto.getPrioridadSeguimiento());

        citaRepository.save(cita);

        // RN-CU11-04: Notificación por correo al agendar seguimiento
        if (cita.getCitaPadreId() != null) {
            String asunto = "Cita de Seguimiento Agendada - Hospital";
            String mensaje = String.format(
                    "Estimado(a) %s,\n\nSe ha agendado una cita de seguimiento de tipo: %s.\n" +
                            "Número de cita: %d\n" +
                            "Fecha: %s\n" +
                            "Médico: %s\n" +
                            "Sucursal: %s\n" +
                            "Motivo: %s\n\n" +
                            "Esta cita se encuentra en estado 'Pendiente de pago'. Para confirmarla, realice el pago " +
                            "en línea desde el portal del paciente (indicando el número de cita) o en la ventanilla de caja " +
                            "de la sucursal antes de la fecha de su cita.\n\n" +
                            "Este es un correo automático, no responda.",
                    paciente.getNombreCompleto(), dto.getTipoSeguimiento(), cita.getId(), cita.getFechaHora().toString(),
                    medico.getNombreCompleto(), sucursal.getNombre(), cita.getMotivo());
            emailService.enviarCorreo(paciente.getCorreo(), asunto, mensaje);
        }

        CitaResponseDTO response = new CitaResponseDTO();
        response.setId(cita.getId());
        response.setPacienteNombre(paciente.getNombreCompleto());
        response.setMedicoNombre(medico.getNombreCompleto());
        response.setSucursalNombre(sucursal.getNombre());
        response.setEspecialidadNombre(especialidad.getNombre());
        response.setEstadoNombre(estadoPendiente.getNombre());
        response.setFechaHora(cita.getFechaHora());
        response.setMotivo(cita.getMotivo());
        response.setCitaPadreId(cita.getCitaPadreId());
        response.setTipoSeguimiento(cita.getTipoSeguimiento());
        response.setPrioridadSeguimiento(cita.getPrioridadSeguimiento());
        return response;
    }

    // Solución CU-16 (gap del QA): las citas nunca se mostraban en el calendario porque
    // no existía forma de consultarlas por médico y rango de fechas; el frontend solo
    // pintaba eventsCache (bloqueos de agenda).
    // Solución QA: CU-16 excluye Pendiente de pago, No Asistió y Cancelada del calendario
    @Override
    public List<CitaResponseDTO> listarCitasPorMedicoYRango(Integer medicoId, LocalDateTime desde, LocalDateTime hasta) {
        List<Cita> citas = citaRepository.findByMedicoIdAndFechaHoraBetweenAndEstado_NombreNotIn(
                medicoId, desde, hasta, ESTADOS_EXCLUIDOS_CALENDARIO);
        return citas.stream().map(this::mapearCitaAResponse).collect(Collectors.toList());
    }

    @Override
    public List<CitaResponseDTO> listarMisCitas(Integer pacienteId) {
        List<Cita> citas = citaRepository.findByPaciente_IdOrderByFechaHoraDesc(pacienteId);
        return citas.stream().map(this::mapearCitaAResponse).collect(Collectors.toList());
    }

    private CitaResponseDTO mapearCitaAResponse(Cita cita) {
        CitaResponseDTO response = new CitaResponseDTO();
        response.setId(cita.getId());
        response.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        response.setMedicoNombre(cita.getMedico().getNombreCompleto());
        response.setSucursalNombre(cita.getSucursal().getNombre());
        response.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        response.setEstadoNombre(cita.getEstado().getNombre());
        response.setFechaHora(cita.getFechaHora());
        response.setMotivo(cita.getMotivo());
        response.setCitaPadreId(cita.getCitaPadreId());
        response.setTipoSeguimiento(cita.getTipoSeguimiento());
        response.setPrioridadSeguimiento(cita.getPrioridadSeguimiento());
        return response;
    }
}