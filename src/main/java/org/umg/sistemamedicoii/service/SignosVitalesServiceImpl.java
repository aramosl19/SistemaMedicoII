package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.CitaEnfermeriaResponseDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesRequestDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.SignosVitales;
import org.umg.sistemamedicoii.models.Usuario;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.SignosVitalesRepository;
import org.umg.sistemamedicoii.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SignosVitalesServiceImpl implements SignosVitalesService {

    // Rangos de captura (RN-CU07-01 a 05)
    private static final int PA_SISTOLICA_MIN = 60;
    private static final int PA_SISTOLICA_MAX = 250;
    private static final int PA_DIASTOLICA_MIN = 40;
    private static final int PA_DIASTOLICA_MAX = 150;

    private static final BigDecimal TEMP_MIN = new BigDecimal("34.0");
    private static final BigDecimal TEMP_MAX = new BigDecimal("42.0");

    private static final BigDecimal PESO_MIN = new BigDecimal("0.5");
    private static final BigDecimal PESO_MAX = new BigDecimal("300");

    private static final BigDecimal TALLA_MIN = new BigDecimal("30");
    private static final BigDecimal TALLA_MAX = new BigDecimal("250");

    private static final int FC_MIN = 30;
    private static final int FC_MAX = 220;

    // Rangos clínicos normales, solo para alertas (RN-CU07-06)
    private static final int PA_SISTOLICA_NORMAL_MIN = 90;
    private static final int PA_SISTOLICA_NORMAL_MAX = 140;
    private static final int PA_DIASTOLICA_NORMAL_MIN = 60;
    private static final int PA_DIASTOLICA_NORMAL_MAX = 90;

    private static final BigDecimal TEMP_NORMAL_MIN = new BigDecimal("36.0");
    private static final BigDecimal TEMP_NORMAL_MAX = new BigDecimal("37.5");

    private static final int FC_NORMAL_MIN = 60;
    private static final int FC_NORMAL_MAX = 100;

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SignosVitalesRepository signosVitalesRepository;
    @Autowired private EstadoCitaCache estadoCache;

    @Override
    public CitaEnfermeriaResponseDTO llamarPaciente(Integer citaId) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.PACIENTE_PRESENTE.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible llamar al paciente: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.SIGNOS_VITALES));
        citaRepository.save(cita);

        CitaEnfermeriaResponseDTO respuesta = new CitaEnfermeriaResponseDTO();
        respuesta.setId(cita.getId());
        respuesta.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        respuesta.setEstadoNombre(cita.getEstado().getNombre());
        respuesta.setEmergencia(cita.isEmergencia());
        respuesta.setMensaje("Turno número " + cita.getId() + ". Paciente " + cita.getPaciente().getNombreCompleto()
                + ", favor pasar a toma de signos vitales.");
        return respuesta;
    }

    @Override
    public SignosVitalesResponseDTO registrarSignosVitales(Integer citaId, SignosVitalesRequestDTO dto) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.SIGNOS_VITALES.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible registrar signos vitales: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        if (signosVitalesRepository.existsByCitaId(citaId)) {
            throw new IllegalArgumentException("Los signos vitales de esta cita ya fueron registrados previamente.");
        }

        Integer enfermeroIdActual = null;
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
            enfermeroIdActual = principal.getUsuario().getId();
        } else {
            throw new org.springframework.security.access.AccessDeniedException("Debe estar autenticado para registrar signos vitales.");
        }

        Usuario enfermero = usuarioRepository.findById(enfermeroIdActual)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el enfermero autenticado."));

        validarRangosCaptura(dto);

        SignosVitales signos = new SignosVitales();
        signos.setCita(cita);
        signos.setEnfermero(enfermero);
        signos.setPresionSistolica(dto.getPresionSistolica());
        signos.setPresionDiastolica(dto.getPresionDiastolica());
        signos.setTemperatura(dto.getTemperatura());
        signos.setPeso(dto.getPeso());
        signos.setTalla(dto.getTalla());
        signos.setFrecuenciaCardiaca(dto.getFrecuenciaCardiaca());
        signos.setFechaRegistro(LocalDateTime.now());

        calcularAlertasClinicas(signos);

        signosVitalesRepository.save(signos);

        cita.setEmergencia(dto.isEmergencia());
        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.EN_ESPERA));
        citaRepository.save(cita);

        return toResponseDTO(signos, dto.isEmergencia());
    }

    private void validarRangosCaptura(SignosVitalesRequestDTO dto) {
        if (dto.getPresionSistolica() == null || dto.getPresionDiastolica() == null
                || dto.getPresionSistolica() < PA_SISTOLICA_MIN || dto.getPresionSistolica() > PA_SISTOLICA_MAX
                || dto.getPresionDiastolica() < PA_DIASTOLICA_MIN || dto.getPresionDiastolica() > PA_DIASTOLICA_MAX) {
            throw new IllegalArgumentException(
                    "La presión arterial debe ingresarse en formato sistólica/diastólica (ej: 120/80) dentro de rangos válidos.");
        }

        if (dto.getTemperatura() == null || dto.getTemperatura().scale() > 1
                || dto.getTemperatura().compareTo(TEMP_MIN) < 0 || dto.getTemperatura().compareTo(TEMP_MAX) > 0) {
            throw new IllegalArgumentException("La temperatura debe estar entre 34.0 y 42.0°C con un decimal.");
        }

        if (dto.getPeso() == null || dto.getPeso().scale() > 2
                || dto.getPeso().compareTo(PESO_MIN) < 0 || dto.getPeso().compareTo(PESO_MAX) > 0) {
            throw new IllegalArgumentException("El peso debe estar entre 0.5 y 300 kg con dos decimales.");
        }

        if (dto.getTalla() == null || dto.getTalla().scale() > 2
                || dto.getTalla().compareTo(TALLA_MIN) < 0 || dto.getTalla().compareTo(TALLA_MAX) > 0) {
            throw new IllegalArgumentException("La talla debe estar entre 30 y 250 cm con dos decimales.");
        }

        if (dto.getFrecuenciaCardiaca() == null
                || dto.getFrecuenciaCardiaca() < FC_MIN || dto.getFrecuenciaCardiaca() > FC_MAX) {
            throw new IllegalArgumentException("La frecuencia cardíaca debe estar entre 30 y 220 latidos por minuto.");
        }
    }

    private void calcularAlertasClinicas(SignosVitales signos) {
        boolean presionNormal = signos.getPresionSistolica() >= PA_SISTOLICA_NORMAL_MIN
                && signos.getPresionSistolica() <= PA_SISTOLICA_NORMAL_MAX
                && signos.getPresionDiastolica() >= PA_DIASTOLICA_NORMAL_MIN
                && signos.getPresionDiastolica() <= PA_DIASTOLICA_NORMAL_MAX;
        signos.setAlertaPresion(!presionNormal);

        boolean temperaturaNormal = signos.getTemperatura().compareTo(TEMP_NORMAL_MIN) >= 0
                && signos.getTemperatura().compareTo(TEMP_NORMAL_MAX) <= 0;
        signos.setAlertaTemperatura(!temperaturaNormal);

        boolean frecuenciaNormal = signos.getFrecuenciaCardiaca() >= FC_NORMAL_MIN
                && signos.getFrecuenciaCardiaca() <= FC_NORMAL_MAX;
        signos.setAlertaFrecuencia(!frecuenciaNormal);
    }

    private SignosVitalesResponseDTO toResponseDTO(SignosVitales signos, boolean emergencia) {
        SignosVitalesResponseDTO respuesta = new SignosVitalesResponseDTO();
        respuesta.setId(signos.getId());
        respuesta.setCitaId(signos.getCita().getId());
        respuesta.setPacienteNombre(signos.getCita().getPaciente().getNombreCompleto());
        respuesta.setPresionSistolica(signos.getPresionSistolica());
        respuesta.setPresionDiastolica(signos.getPresionDiastolica());
        respuesta.setTemperatura(signos.getTemperatura());
        respuesta.setPeso(signos.getPeso());
        respuesta.setTalla(signos.getTalla());
        respuesta.setFrecuenciaCardiaca(signos.getFrecuenciaCardiaca());
        respuesta.setAlertaPresion(signos.isAlertaPresion());
        respuesta.setAlertaTemperatura(signos.isAlertaTemperatura());
        respuesta.setAlertaFrecuencia(signos.isAlertaFrecuencia());
        respuesta.setEmergencia(emergencia);
        respuesta.setFechaRegistro(signos.getFechaRegistro());

        String nombrePaciente = signos.getCita().getPaciente().getNombreCompleto();
        if (emergencia) {
            respuesta.setMensaje("Signos vitales de emergencia registrados para el paciente " + nombrePaciente
                    + ". Será notificado al médico con prioridad de atención inmediata.");
        } else {
            respuesta.setMensaje("Signos vitales del paciente " + nombrePaciente
                    + " registrados correctamente. El paciente puede regresar a la sala de espera.");
        }
        return respuesta;
    }
}