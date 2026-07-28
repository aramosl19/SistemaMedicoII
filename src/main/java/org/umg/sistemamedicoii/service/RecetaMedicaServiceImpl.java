package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.DetalleRecetaRequestDTO;
import org.umg.sistemamedicoii.dto.DetalleRecetaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaRequestDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.DetalleReceta;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.models.RecetaMedica;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.RecetaMedicaRepository;

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
                throw new IllegalArgumentException("Debe seleccionar un medicamento válido.");
            }
            Medicamento medicamento = medicamentoRepository.findById(item.getMedicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el medicamento con id " + item.getMedicamentoId() + "."));
            if (!medicamento.isActivo()) {
                throw new IllegalArgumentException("El medicamento " + medicamento.getNombre() + " no está disponible.");
            }
            if (isBlank(item.getDosis()) || isBlank(item.getFrecuencia()) || isBlank(item.getDuracion())) {
                throw new IllegalArgumentException("Dosis, frecuencia y duración son obligatorios para cada medicamento.");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad del medicamento " + medicamento.getNombre() + " debe ser mayor a 0.");
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

        return toResponseDTO(receta, "Receta médica generada exitosamente. El paciente puede pasar a farmacia.");
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