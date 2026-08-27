package org.umg.sistemamedicoii.service.examenes_laboratorio.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.examenes_laboratorio.DetalleOrdenLaboratorio;
import org.umg.sistemamedicoii.models.examenes_laboratorio.ExamenLaboratorio;
import org.umg.sistemamedicoii.models.examenes_laboratorio.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.ExamenLaboratorioRepository;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.OrdenLaboratorioRepository;
import org.umg.sistemamedicoii.service.examenes_laboratorio.OrdenLaboratorioService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrdenLaboratorioServiceImpl implements OrdenLaboratorioService {

    @Autowired private CitaRepository citaRepository;
    @Autowired private ExamenLaboratorioRepository examenLaboratorioRepository;
    @Autowired private OrdenLaboratorioRepository ordenLaboratorioRepository;

    @Override
    public OrdenLaboratorioResponseDTO generarOrden(Integer citaId, OrdenLaboratorioRequestDTO dto) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();
        if (!EstadoCitaEnum.EVALUADO.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible generar una orden de laboratorio: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        if (dto.getExamenesIds() == null || dto.getExamenesIds().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un examen para generar la orden.");
        }

        OrdenLaboratorio orden = new OrdenLaboratorio();
        orden.setCita(cita);
        orden.setMedico(cita.getMedico());
        orden.setEsExterna(dto.isEsExterna());
        orden.setNotas(dto.getNotas());
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setEstado(EstadoOrdenLaboratorioEnum.PENDIENTE);

        BigDecimal montoTotal = BigDecimal.ZERO;
        for (Integer examenId : dto.getExamenesIds()) {
            ExamenLaboratorio examen = examenLaboratorioRepository.findById(examenId)
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen con id " + examenId + "."));

            DetalleOrdenLaboratorio detalle = new DetalleOrdenLaboratorio();
            detalle.setOrden(orden);
            detalle.setExamen(examen);
            detalle.setMonto(examen.getPrecio());

            // Prellena el rango y la unidad para el laboratorista
            detalle.setRangoReferencia(examen.getRangoReferencia());
            detalle.setUnidad(examen.getUnidadMedida());

            orden.getDetalles().add(detalle);
            montoTotal = montoTotal.add(examen.getPrecio());
        }
        orden.setMontoTotal(montoTotal);

        ordenLaboratorioRepository.save(orden);

        return toResponseDTO(orden);
    }

    private OrdenLaboratorioResponseDTO toResponseDTO(OrdenLaboratorio orden) {
        OrdenLaboratorioResponseDTO dto = new OrdenLaboratorioResponseDTO();
        dto.setId(orden.getId());
        dto.setCitaId(orden.getCita().getId());
        dto.setPacienteNombre(orden.getCita().getPaciente().getNombreCompleto());
        dto.setMedicoNombre(orden.getMedico().getNombreCompleto());
        dto.setEspecialidadNombre(orden.getCita().getEspecialidad().getNombre());
        dto.setFechaHora(orden.getCita().getFechaHora());
        dto.setEstado(orden.getEstado().getNombre());
        dto.setEsExterna(orden.isEsExterna());
        dto.setMontoTotal(orden.getMontoTotal());
        dto.setNotas(orden.getNotas());
        dto.setFechaCreacion(orden.getFechaCreacion());

        List<DetalleOrdenLaboratorioResponseDTO> examenes = orden.getDetalles().stream()
                .map(detalle -> {
                    DetalleOrdenLaboratorioResponseDTO d = new DetalleOrdenLaboratorioResponseDTO();
                    d.setId(detalle.getId());
                    d.setExamenNombre(detalle.getExamen().getNombre());
                    d.setMonto(detalle.getMonto());
                    d.setPublicado(detalle.isPublicado());
                    return d;
                })
                .collect(Collectors.toList());
        dto.setExamenes(examenes);

        // FIX CU-08 (mock/QA texto): el CU-08 (FA01) exige literalmente
        // "Orden de laboratorio generada exitosamente. Número de orden: [Número].
        // Exámenes: [lista]. El paciente debe dirigirse al área de laboratorio."
        // Antes se mostraba el monto y se enviaba a caja, dato/flujo que el CU-08 no menciona.
        String listaExamenes = examenes.stream()
                .map(DetalleOrdenLaboratorioResponseDTO::getExamenNombre)
                .collect(Collectors.joining(", "));

        dto.setMensaje("Orden de laboratorio generada exitosamente. Número de orden: " + orden.getId()
                + ". Exámenes: " + listaExamenes + ". El paciente debe dirigirse al área de laboratorio.");
        return dto;
    }
}