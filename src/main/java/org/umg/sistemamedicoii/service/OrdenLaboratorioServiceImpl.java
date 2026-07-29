package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.DetalleOrdenLaboratorio;
import org.umg.sistemamedicoii.models.ExamenLaboratorio;
import org.umg.sistemamedicoii.models.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.ExamenLaboratorioRepository;
import org.umg.sistemamedicoii.repository.OrdenLaboratorioRepository;

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

        dto.setMensaje("Orden de laboratorio generada exitosamente por un monto de Q" + orden.getMontoTotal()
                + ". El paciente debe dirigirse a caja para realizar el pago antes de la toma de muestras.");
        return dto;
    }
}