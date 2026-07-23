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

    //Horario fijo (debo preguntarle al ingeniero si esta bien o debo cambiarlo el sabado.)

    private static final LocalTime HORA_INICIO = LocalTime.of(8,0);
    private static final LocalTime HORA_FIN = LocalTime.of(17,0);
    private static final int DURACION_MINUTOS = 30;
    private static final int MINUTOS_RESERVA = 5;

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CitaRepository citaRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private EstadoCitaRepository estadoCitaRepository;

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

        List<Cita> ocupadas = citaRepository.findByMedicoIdAndFechaHoraBetween(
                medicoId, fecha.atTime(HORA_INICIO), fin);
        List<LocalDateTime> ocupadosList = ocupadas.stream().map(Cita::getFechaHora).collect(Collectors.toList());

        return slots.stream()
                .filter(slot->!ocupadosList.contains(slot))
                .collect(Collectors.toList());
    }

    @Override
    public CitaResponseDTO agendarCita(CitaRequestDTO dto) {
        if (!dto.getFechaHora().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");
        }

        if (citaRepository.existsByMedicoIdAndFechaHora(dto.getMedicoId(),dto.getFechaHora())){
            throw new IllegalArgumentException("El horario seleccionado ya no esta disponible. Por favor, elija otro horario.");
        }

        Usuario paciente = usuarioRepository.findById(dto.getPacienteId())
                .orElseThrow(()-> new ResourceNotFoundException("Paciente no encontrado."));
        if (paciente.getRol() == null || !"Paciente".equalsIgnoreCase(paciente.getRol().getNombre())) {
            throw new IllegalArgumentException("El paciente seleccionado no es válido.");
        }
        Usuario medico = usuarioRepository.findById(dto.getMedicoId())
                .orElseThrow(()-> new ResourceNotFoundException("Médico no encontrado."));
        if (medico.getRol() == null || !"Médico".equalsIgnoreCase(medico.getRol().getNombre())) {
            throw new IllegalArgumentException("El médico seleccionado no es válido.");
        }
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(()-> new ResourceNotFoundException("Sucursal no encontrada."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(()-> new ResourceNotFoundException("Especialidad no encontrada."));
        EstadoCita estadoPendiente = estadoCitaRepository.findAll().stream()
                .filter(e -> "Pendiente de pago".equalsIgnoreCase(e.getNombre()))
                .findFirst()
                .orElseThrow(()-> new ResourceNotFoundException("Estado 'Pendiente de pago' no configurado."));





        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setEstado(estadoPendiente);
        cita.setFechaHora(dto.getFechaHora());
        cita.setMotivo(dto.getMotivo());
        cita.setReservadaHasta(LocalDateTime.now().plusMinutes(MINUTOS_RESERVA));

        citaRepository.save(cita);

        CitaResponseDTO response = new CitaResponseDTO();
        response.setId(cita.getId());
        response.setPacienteNombre(paciente.getNombreCompleto());
        response.setMedicoNombre(medico.getNombreCompleto());
        response.setSucursalNombre(sucursal.getNombre());
        response.setEspecialidadNombre(especialidad.getNombre());
        response.setEstadoNombre(estadoPendiente.getNombre());
        response.setFechaHora(cita.getFechaHora());
        response.setMotivo(cita.getMotivo());
        return response;
    }
}
