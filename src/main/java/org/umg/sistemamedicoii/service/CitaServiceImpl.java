package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.*;
import org.umg.sistemamedicoii.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CitaServiceImpl implements CitaService {
    private static final String ESTADO_CANCELADA = "Cancelada";
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
    @Autowired private org.umg.sistemamedicoii.config.EstadoCitaCache estadoCache;


    @Override
    public List<MedicoDisponibleResponseDTO> listarMedicosDisponibles(Integer sucursalId, Integer especialidadId){
        return usuarioRepository.findAll().stream()
                .filter(u->u.isActivo()
                        && u.getRol() != null && "Médico".equalsIgnoreCase(u.getRol().getNombre())
                        && sucursalId.equals(u.getSucursal()!= null ? u.getSucursal().getId():null)
                        && especialidadId.equals(u.getEspecialidad() != null ? u.getEspecialidad().getId():null))
                .map(u-> {
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

        return slots.stream()
                .filter(slot->!ocupadosList.contains(slot))
                .collect(Collectors.toList());
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
        cita.setReservadaHasta(LocalDateTime.now().plusMinutes(MINUTOS_RESERVA));
        cita.setFechaCreacion(LocalDateTime.now());
        cita.setCreadaPorPersonalInterno(creadaPorPersonalInterno);

        // Asignación de datos de seguimiento
        cita.setCitaPadreId(dto.getCitaPadreId());
        cita.setTipoSeguimiento(dto.getTipoSeguimiento());

        citaRepository.save(cita);

        // RN-CU11-04: Notificación por correo al agendar seguimiento
        if (cita.getCitaPadreId() != null) {
            String asunto = "Cita de Seguimiento Agendada - Hospital";
            String mensaje = String.format("Estimado(a) %s,\n\nSe ha agendado una cita de seguimiento de tipo: %s.\nFecha: %s\nMédico: %s\nSucursal: %s\nMotivo: %s\n\nEste es un correo automático, no responda.",
                    paciente.getNombreCompleto(), dto.getTipoSeguimiento(), cita.getFechaHora().toString(), medico.getNombreCompleto(), sucursal.getNombre(), cita.getMotivo());
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
        return response;
    }
}