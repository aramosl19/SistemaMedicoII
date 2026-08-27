package org.umg.sistemamedicoii.service.atencion_medica_enfermeria.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.DetalleRecetaRequestDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.DetalleRecetaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaRequestDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.DetalleReceta;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.Medicamento;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.RecetaMedica;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.atencion_medica_enfermeria.RecetaMedicaRepository;
import org.umg.sistemamedicoii.service.atencion_medica_enfermeria.RecetaMedicaService;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class RecetaMedicaServiceImpl implements RecetaMedicaService {

    @Autowired private CitaRepository citaRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;
    @Autowired private RecetaMedicaRepository recetaMedicaRepository;

    @Override
    public RecetaMedicaResponseDTO generarReceta(Integer citaId, RecetaRequestDTO dto) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();
        if (!EstadoCitaEnum.EVALUADO.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException(
                    "No es posible generar una receta: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        if (dto.getMedicamentos() == null || dto.getMedicamentos().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un medicamento para la receta.");
        }

        RecetaMedica receta = new RecetaMedica();
        receta.setCita(cita);
        receta.setMedico(cita.getMedico());
        receta.setFechaEmision(LocalDateTime.now());
        receta.setNotas(dto.getNotas());
        receta.setActivo(true);

        for (DetalleRecetaRequestDTO item : dto.getMedicamentos()) {
            if (item.getMedicamentoId() == null) {
                throw new IllegalArgumentException("Todos los campos de la receta son obligatorios excepto indicaciones especiales.");
            }
            Medicamento medicamento = medicamentoRepository.findById(item.getMedicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el medicamento con id " + item.getMedicamentoId() + "."));
            if (!medicamento.isActivo()) {
                throw new IllegalArgumentException("El medicamento " + medicamento.getNombre() + " no está disponible.");
            }
            // FIX CU-08 (mock/QA texto): RN-CU08-03 exige un único mensaje para
            // cualquier campo obligatorio de la receta que falte (excepto indicaciones).
            if (isBlank(item.getDosis()) || isBlank(item.getFrecuencia()) || isBlank(item.getDuracion())
                    || item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("Todos los campos de la receta son obligatorios excepto indicaciones especiales.");
            }

            DetalleReceta detalle = new DetalleReceta();
            detalle.setReceta(receta);
            detalle.setMedicamento(medicamento);
            detalle.setDosis(item.getDosis());
            detalle.setFrecuencia(item.getFrecuencia());
            detalle.setDuracion(item.getDuracion());
            detalle.setIndicaciones(item.getIndicaciones());
            detalle.setCantidad(item.getCantidad());
            receta.getDetalles().add(detalle);
        }

        recetaMedicaRepository.save(receta);

        // FIX CU-08 (mock/QA texto): el CU-08 (FA04) exige literalmente
        // "Receta médica generada exitosamente. Medicamentos: [lista]. El paciente
        // puede adquirirlos en la farmacia de la clínica."
        String listaMedicamentos = receta.getDetalles().stream()
                .map(d -> d.getMedicamento().getNombre())
                .collect(Collectors.joining(", "));

        return toResponseDTO(receta, "Receta médica generada exitosamente. Medicamentos: " + listaMedicamentos
                + ". El paciente puede adquirirlos en la farmacia de la clínica.");
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.isBlank();
    }

    private RecetaMedicaResponseDTO toResponseDTO(RecetaMedica receta, String mensaje) {
        RecetaMedicaResponseDTO dto = new RecetaMedicaResponseDTO();
        dto.setId(receta.getId());
        dto.setCitaId(receta.getCita().getId());
        dto.setPacienteNombre(receta.getCita().getPaciente().getNombreCompleto());
        dto.setMedicoNombre(receta.getMedico().getNombreCompleto());
        dto.setFechaEmision(receta.getFechaEmision());
        dto.setNotas(receta.getNotas());
        dto.setActivo(receta.isActivo());
        dto.setMedicamentos(receta.getDetalles().stream().map(d -> {
            DetalleRecetaResponseDTO det = new DetalleRecetaResponseDTO();
            det.setId(d.getId());
            det.setMedicamentoId(d.getMedicamento().getId());
            det.setMedicamentoNombre(d.getMedicamento().getNombre());
            det.setDosis(d.getDosis());
            det.setFrecuencia(d.getFrecuencia());
            det.setDuracion(d.getDuracion());
            det.setIndicaciones(d.getIndicaciones());
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(d.getMedicamento().getPrecio());
            det.setSubtotal(d.getMedicamento().getPrecio().multiply(java.math.BigDecimal.valueOf(d.getCantidad())));
            return det;
        }).collect(Collectors.toList()));
        dto.setMensaje(mensaje);
        return dto;
    }
}