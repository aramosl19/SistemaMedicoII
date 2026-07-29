package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.ResultadoBusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.Usuario;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecepcionServiceImpl implements RecepcionService {

    private static final String ESTADO_PENDIENTE_PAGO = "Pendiente de pago";
    private static final String ESTADO_CONFIRMADA = "Confirmada";
    private static final String ESTADO_CANCELADA = "Cancelada";
    private static final String ESTADO_PACIENTE_PRESENTE = "Paciente Presente";

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private org.umg.sistemamedicoii.config.EstadoCitaCache estadoCache;
    @Autowired private org.umg.sistemamedicoii.repository.AuditoriaRepository auditoriaRepo;

    @Override
    public BusquedaRecepcionResponseDTO buscar(Integer numeroCita, String dpi) {

        if (numeroCita == null && (dpi == null || dpi.isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un número de cita o DPI para buscar.");
        }

        if (numeroCita != null) {
            Cita cita = citaRepository.findById(numeroCita)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));
            return construirEncontrada(cita);
        }

        Usuario paciente = usuarioRepository.findByDpi(dpi).orElse(null);

        if (paciente == null) {
            BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
            respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.PACIENTE_NO_REGISTRADO);
            return respuesta;
        }

        List<Cita> citasActivas = citaRepository
                .findByPaciente_IdAndEstado_NombreNotOrderByFechaHoraAsc(paciente.getId(), ESTADO_CANCELADA);

        Cita citaActiva = citasActivas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(LocalDate.now()))
                .findFirst()
                .orElse(citasActivas.isEmpty() ? null : citasActivas.get(0));

        if (citaActiva == null) {
            BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
            respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.SIN_CITAS_ACTIVAS);
            respuesta.setPacienteNombre(paciente.getNombreCompleto());
            respuesta.setPacienteId(paciente.getId());
            return respuesta;
        }

        return construirEncontrada(citaActiva);
    }

    @Override
    public CitaRecepcionResponseDTO registrarLlegada(Integer citaId, boolean emergencia) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();

        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.CANCELADA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.PENDIENTE_PAGO.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La cita tiene estado 'Pendiente de pago'. Debe realizar el pago en caja.");
        }
        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La llegada de este paciente ya fue registrada previamente.");
        }
        if (!org.umg.sistemamedicoii.enums.EstadoCitaEnum.CONFIRMADA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("No es posible registrar la llegada: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        // Usamos el caché aquí
        cita.setEstado(estadoCache.getEstado(org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE));
        cita.setHoraLlegada(LocalDateTime.now());
        cita.setEmergencia(emergencia);
        citaRepository.save(cita);

        CitaRecepcionResponseDTO respuesta = toRecepcionDTO(cita);
        respuesta.setMensaje(emergencia
                ? "Paciente " + cita.getPaciente().getNombreCompleto() + " registrado con prioridad de EMERGENCIA. Debe pasar directamente a toma de signos vitales."
                : "La llegada del paciente " + cita.getPaciente().getNombreCompleto() + " ha sido registrada exitosamente. Debe pasar a la sala de espera.");
        return respuesta;
    }

    @Override
    public CitaRecepcionResponseDTO reasignarMedico(Integer citaId, org.umg.sistemamedicoii.dto.ReasignarMedicoRequestDTO dto) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        String estado = cita.getEstado().getNombre();
        String estadoConfirmada = org.umg.sistemamedicoii.enums.EstadoCitaEnum.CONFIRMADA.getNombreBd();
        String estadoPresente = org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE.getNombreBd();
        if (!estado.equals(estadoConfirmada) && !estado.equals(estadoPresente)) {
            throw new IllegalArgumentException("Solo se pueden reasignar citas Confirmadas o con Paciente Presente.");
        }
        Usuario nuevoMedico = usuarioRepository.findById(dto.getNuevoMedicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));
        if (nuevoMedico.getSucursal() == null || !nuevoMedico.getSucursal().getId().equals(cita.getSucursal().getId()) ||
                nuevoMedico.getEspecialidad() == null || !nuevoMedico.getEspecialidad().getId().equals(cita.getEspecialidad().getId())) {
            throw new IllegalArgumentException("El nuevo médico debe ser de la misma sede y especialidad.");
        }
        // Validación anti-doble-booking
        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                nuevoMedico.getId(), cita.getFechaHora(), org.umg.sistemamedicoii.enums.EstadoCitaEnum.CANCELADA.getNombreBd())) {
            throw new IllegalArgumentException("El nuevo médico no tiene disponibilidad en este horario.");
        }
        Integer medicoAnteriorId = cita.getMedico().getId();
        cita.setMedico(nuevoMedico);
        citaRepository.save(cita);
        // Guardamos la justificación administrativa en la tabla de auditoría, NO en el motivo clínico.
        org.umg.sistemamedicoii.models.Auditoria log = new org.umg.sistemamedicoii.models.Auditoria();
        log.setAccion("REASIGNACION_MEDICO");
        log.setEntidadAfectada("CITA");
        log.setEntidadId(cita.getId());
        log.setDetalle("Médico anterior: " + medicoAnteriorId + ". Nuevo médico: " + nuevoMedico.getId() + ". Motivo: " + (dto.getMotivoReasignacion() != null ? dto.getMotivoReasignacion() : "Sin especificar"));
        log.setUsuarioEjecutorId(null);
        log.setFechaHora(LocalDateTime.now());
        auditoriaRepo.save(log);
        CitaRecepcionResponseDTO res = toRecepcionDTO(cita);
        res.setMensaje("Médico reasignado correctamente.");
        return res;
    }

    private BusquedaRecepcionResponseDTO construirEncontrada(Cita cita) {
        BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
        respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.CITA_ENCONTRADA);
        respuesta.setCita(toRecepcionDTO(cita));
        return respuesta;
    }

    private CitaRecepcionResponseDTO toRecepcionDTO(Cita cita) {
        CitaRecepcionResponseDTO dto = new CitaRecepcionResponseDTO();
        dto.setId(cita.getId());
        dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        dto.setEstadoNombre(cita.getEstado().getNombre());
        dto.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        dto.setSucursalNombre(cita.getSucursal().getNombre());
        dto.setFechaHora(cita.getFechaHora());
        dto.setMotivo(cita.getMotivo());
        dto.setEmergencia(cita.isEmergencia());
        dto.setHoraLlegada(cita.getHoraLlegada());
        return dto;
    }
}