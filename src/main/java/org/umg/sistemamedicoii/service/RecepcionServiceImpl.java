package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.ResultadoBusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.EstadoCita;
import org.umg.sistemamedicoii.models.Usuario;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.EstadoCitaRepository;
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
    @Autowired private EstadoCitaRepository estadoCitaRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente."));

        String estadoActual = cita.getEstado().getNombre();

        if (ESTADO_CANCELADA.equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }

        if (ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
        }

        if (ESTADO_PACIENTE_PRESENTE.equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La llegada de este paciente ya fue registrada previamente.");
        }

        if (!ESTADO_CONFIRMADA.equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible registrar la llegada: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        EstadoCita pacientePresente = estadoCitaRepository.findByNombre(ESTADO_PACIENTE_PRESENTE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estado 'Paciente Presente' no configurado en el catálogo estado_cita."));

        cita.setEstado(pacientePresente);
        cita.setHoraLlegada(LocalDateTime.now());
        cita.setEmergencia(emergencia);
        citaRepository.save(cita);

        CitaRecepcionResponseDTO respuesta = toRecepcionDTO(cita);
        respuesta.setMensaje(emergencia
                ? "Paciente " + cita.getPaciente().getNombreCompleto()
                  + " registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales."
                : "La llegada del paciente " + cita.getPaciente().getNombreCompleto()
                  + " ha sido registrada exitosamente. El paciente debe pasar a la sala de espera.");
        return respuesta;
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