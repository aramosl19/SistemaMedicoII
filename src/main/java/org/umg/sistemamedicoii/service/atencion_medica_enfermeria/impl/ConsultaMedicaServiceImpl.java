package org.umg.sistemamedicoii.service.atencion_medica_enfermeria.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.cache.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.*;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.CatalogoCie10;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.ConsultaMedica;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoCie10Repository;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.atencion_medica_enfermeria.ConsultaMedicaRepository;
import org.umg.sistemamedicoii.service.atencion_medica_enfermeria.ConsultaMedicaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultaMedicaServiceImpl implements ConsultaMedicaService {

    private static final int DIAGNOSTICO_MIN = 10;
    private static final int DIAGNOSTICO_MAX = 5000;

    @Autowired private CitaRepository citaRepository;
    @Autowired private ConsultaMedicaRepository consultaMedicaRepository;
    @Autowired private CatalogoCie10Repository catalogoCie10Repository;
    @Autowired private EstadoCitaCache estadoCache;

    @Override
    public PanelMedicoResponseDTO obtenerPanel(Integer medicoId) {
        PanelMedicoResponseDTO panel = new PanelMedicoResponseDTO();
        panel.setEnEsperaDeConsulta(listarPorEstado(medicoId, EstadoCitaEnum.EN_ESPERA));
        panel.setEnConsultaMedica(listarPorEstado(medicoId, EstadoCitaEnum.CONSULTA_MEDICA));
        panel.setEvaluadosPendienteCierre(listarPorEstado(medicoId, EstadoCitaEnum.EVALUADO));
        return panel;
    }

    private List<CitaPanelMedicoResponseDTO> listarPorEstado(Integer medicoId, EstadoCitaEnum estado) {
        return citaRepository
                .findByMedicoIdAndEstado_NombreOrderByEmergenciaDescFechaHoraAsc(medicoId, estado.getNombreBd())
                .stream()
                .map(this::toPanelDTO)
                .collect(Collectors.toList());
    }

    private CitaPanelMedicoResponseDTO toPanelDTO(Cita cita) {
        CitaPanelMedicoResponseDTO dto = new CitaPanelMedicoResponseDTO();
        dto.setId(cita.getId());
        dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        dto.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        dto.setFechaHora(cita.getFechaHora());
        dto.setEstadoNombre(cita.getEstado().getNombre());
        dto.setEmergencia(cita.isEmergencia());
        return dto;
    }

    @Override
    public CitaConsultaResponseDTO iniciarConsulta(Integer citaId) {
        Cita cita = buscarCita(citaId);
        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.EN_ESPERA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible iniciar la consulta: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.CONSULTA_MEDICA));
        citaRepository.save(cita);

        return toCitaConsultaDTO(cita, "Turno número " + cita.getId() + ". Paciente "
                + cita.getPaciente().getNombreCompleto() + ", favor pasar a consulta médica.");
    }

    @Override
    public CitaConsultaResponseDTO marcarNoAsistio(Integer citaId) {
        Cita cita = buscarCita(citaId);
        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.EN_ESPERA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible marcar 'No Asistió': la cita se encuentra en estado '" + estadoActual + "'.");
        }

        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.NO_ASISTIO));
        citaRepository.save(cita);

        return toCitaConsultaDTO(cita, "Cita #" + cita.getId() + " marcada como No Asistió.");
    }

    @Override
    public ConsultaMedicaResponseDTO guardarConsulta(Integer citaId, ConsultaMedicaRequestDTO dto) {
        Cita cita = buscarCita(citaId);
        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.CONSULTA_MEDICA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible registrar la consulta: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        ConsultaMedica consulta = consultaMedicaRepository.findByCitaId(citaId)
                .orElseGet(() -> {
                    ConsultaMedica nueva = new ConsultaMedica();
                    nueva.setCita(cita);
                    nueva.setMedico(cita.getMedico());
                    nueva.setFechaInicio(LocalDateTime.now());
                    return nueva;
                });

        if (consulta.isFinalizada()) {
            throw new IllegalArgumentException("La consulta ya fue finalizada y no puede modificarse.");
        }

        consulta.setMotivoVisita(dto.getMotivoVisita());
        consulta.setHallazgosClinicos(dto.getHallazgosClinicos());
        consulta.setPlanTratamiento(dto.getPlanTratamiento());
        consulta.setNotasAdicionales(dto.getNotasAdicionales());
        consulta.setDiagnostico(dto.getDiagnostico());

        if (dto.getCie10Id() != null) {
            CatalogoCie10 cie10 = catalogoCie10Repository.findById(dto.getCie10Id())
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el código CIE-10 ingresado."));
            consulta.setCie10(cie10);
        }

        String mensaje;
        if (dto.isFinalizar()) {
            validarCierre(dto);
            consulta.setFinalizada(true);
            consulta.setFechaCierre(LocalDateTime.now());
            cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.EVALUADO));
            citaRepository.save(cita);
            mensaje = "La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.";
        } else {
            mensaje = "Avance de la consulta guardado correctamente.";
        }

        consultaMedicaRepository.save(consulta);

        return toConsultaResponseDTO(consulta, mensaje);
    }

    @Override
    public ConsultaMedicaResponseDTO obtenerBorrador(Integer citaId) {
        return consultaMedicaRepository.findByCitaId(citaId)
                .map(consulta -> toConsultaResponseDTO(consulta, null))
                .orElse(null);
    }

    @Override
    public CitaConsultaResponseDTO finalizarAtencion(Integer citaId) {
        Cita cita = buscarCita(citaId);
        String estadoActual = cita.getEstado().getNombre();

        if (!EstadoCitaEnum.EVALUADO.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible finalizar la atención: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.ATENCION_FINALIZADA));
        citaRepository.save(cita);

        return toCitaConsultaDTO(cita, "Atención finalizada para cita #" + cita.getId() + ".");
    }

    // FIX CU-08 (mock/QA texto): dos mensajes distintos que coexisten:
    // - Diagnóstico vacío al finalizar -> texto literal de FA05.
    // - Diagnóstico presente pero fuera de 10-5000 caracteres -> texto literal de RN-CU08-01.
    private void validarCierre(ConsultaMedicaRequestDTO dto) {
        boolean camposBasicosCompletos = esNoVacio(dto.getMotivoVisita())
                && esNoVacio(dto.getHallazgosClinicos())
                && esNoVacio(dto.getPlanTratamiento());

        if (!camposBasicosCompletos) {
            throw new IllegalArgumentException("Debe completar todos los campos obligatorios para cerrar la consulta.");
        }

        String diagnostico = dto.getDiagnostico();
        if (diagnostico == null || diagnostico.isBlank()) {
            throw new IllegalArgumentException(
                    "No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.");
        }
        if (diagnostico.length() < DIAGNOSTICO_MIN || diagnostico.length() > DIAGNOSTICO_MAX) {
            throw new IllegalArgumentException(
                    "El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.");
        }
    }

    private boolean esNoVacio(String valor) {
        return valor != null && !valor.isBlank();
    }

    private Cita buscarCita(Integer citaId) {
        return citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));
    }

    private CitaConsultaResponseDTO toCitaConsultaDTO(Cita cita, String mensaje) {
        CitaConsultaResponseDTO dto = new CitaConsultaResponseDTO();
        dto.setId(cita.getId());
        dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        dto.setEstadoNombre(cita.getEstado().getNombre());
        dto.setEmergencia(cita.isEmergencia());
        dto.setMensaje(mensaje);
        return dto;
    }

    private ConsultaMedicaResponseDTO toConsultaResponseDTO(ConsultaMedica consulta, String mensaje) {
        ConsultaMedicaResponseDTO dto = new ConsultaMedicaResponseDTO();
        dto.setId(consulta.getId());
        dto.setCitaId(consulta.getCita().getId());
        dto.setPacienteNombre(consulta.getCita().getPaciente().getNombreCompleto());
        dto.setMedicoNombre(consulta.getMedico().getNombreCompleto());
        dto.setMotivoVisita(consulta.getMotivoVisita());
        dto.setHallazgosClinicos(consulta.getHallazgosClinicos());
        dto.setCie10Codigo(consulta.getCie10() != null ? consulta.getCie10().getCodigo() : null);
        dto.setDiagnostico(consulta.getDiagnostico());
        dto.setPlanTratamiento(consulta.getPlanTratamiento());
        dto.setNotasAdicionales(consulta.getNotasAdicionales());
        dto.setFinalizada(consulta.isFinalizada());
        dto.setFechaInicio(consulta.getFechaInicio());
        dto.setFechaCierre(consulta.getFechaCierre());
        dto.setMensaje(mensaje);
        return dto;
    }
}