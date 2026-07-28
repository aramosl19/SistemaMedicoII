package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.RegistrarResultadoRequestDTO;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.DetalleOrdenLaboratorio;
import org.umg.sistemamedicoii.models.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.DetalleOrdenLaboratorioRepository;
import org.umg.sistemamedicoii.repository.OrdenLaboratorioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LaboratorioResultadoServiceImpl implements LaboratorioResultadoService {

    @Autowired private OrdenLaboratorioRepository ordenLaboratorioRepository;
    @Autowired private DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;

    @Override
    public List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado) {
        EstadoOrdenLaboratorioEnum filtro = parsearEstado(estado);
        return ordenLaboratorioRepository.findByEstadoOrderByFechaCreacionAsc(filtro).stream()
                .map(this::toOrdenResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        return toOrdenResponseDTO(orden);
    }

    @Override
    public DetalleOrdenLaboratorioResponseDTO registrarResultado(Integer detalleId, RegistrarResultadoRequestDTO dto) {
        DetalleOrdenLaboratorio detalle = buscarDetalle(detalleId);
        OrdenLaboratorio orden = detalle.getOrden();

        if (orden.getEstado() != EstadoOrdenLaboratorioEnum.EN_PROCESO) {
            throw new IllegalArgumentException(
                    "No es posible registrar resultados: la orden se encuentra en estado '" + orden.getEstado().getNombre() + "'.");
        }

        if (dto.getUnidad() == null || dto.getUnidad().isBlank()) {
            throw new IllegalArgumentException("La unidad del resultado es obligatoria.");
        }
        if (dto.getRangoReferencia() == null || dto.getRangoReferencia().isBlank()) {
            throw new IllegalArgumentException("El rango de referencia del examen es obligatorio.");
        }

        detalle.setValorResultado(dto.getValorResultado());
        detalle.setUnidad(dto.getUnidad());
        detalle.setRangoReferencia(dto.getRangoReferencia());
        detalle.setFueraDeRango(dto.isFueraDeRango());
        detalle.setNotasResultado(dto.getNotasResultado());
        detalle.setFechaResultado(LocalDateTime.now());

        detalleOrdenLaboratorioRepository.save(detalle);

        String mensaje = detalle.isFueraDeRango()
                ? "Resultado guardado exitosamente. Los resultados están fuera del rango de referencia normal. Requiere revisión."
                : "Resultado guardado exitosamente.";

        return toDetalleResponseDTO(detalle, mensaje);
    }

    @Override
    public DetalleOrdenLaboratorioResponseDTO publicarResultado(Integer detalleId) {
        DetalleOrdenLaboratorio detalle = buscarDetalle(detalleId);

        if (detalle.getValorResultado() == null) {
            throw new IllegalArgumentException("No se puede publicar un examen sin resultado registrado.");
        }
        if (detalle.isPublicado()) {
            throw new IllegalArgumentException("Este resultado ya fue publicado previamente.");
        }

        detalle.setPublicado(true);
        detalleOrdenLaboratorioRepository.save(detalle);

        OrdenLaboratorio orden = detalle.getOrden();
        boolean todosPublicados = orden.getDetalles().stream().allMatch(DetalleOrdenLaboratorio::isPublicado);
        if (todosPublicados) {
            orden.setEstado(EstadoOrdenLaboratorioEnum.COMPLETADA);
            ordenLaboratorioRepository.save(orden);
        }

        return toDetalleResponseDTO(detalle, "Resultado publicado exitosamente.");
    }

    private EstadoOrdenLaboratorioEnum parsearEstado(String estado) {
        try {
            return EstadoOrdenLaboratorioEnum.valueOf(estado.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Estado de orden no válido: " + estado);
        }
    }

    private OrdenLaboratorio buscarOrden(Integer ordenId) {
        return ordenLaboratorioRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio ingresada."));
    }

    private DetalleOrdenLaboratorio buscarDetalle(Integer detalleId) {
        return detalleOrdenLaboratorioRepository.findById(detalleId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el examen ingresado."));
    }

    private OrdenLaboratorioResponseDTO toOrdenResponseDTO(OrdenLaboratorio orden) {
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
        dto.setExamenes(orden.getDetalles().stream()
                .map(d -> toDetalleResponseDTO(d, null))
                .collect(Collectors.toList()));
        return dto;
    }

    private DetalleOrdenLaboratorioResponseDTO toDetalleResponseDTO(DetalleOrdenLaboratorio detalle, String mensaje) {
        DetalleOrdenLaboratorioResponseDTO dto = new DetalleOrdenLaboratorioResponseDTO();
        dto.setId(detalle.getId());
        dto.setExamenNombre(detalle.getExamen().getNombre());
        dto.setMonto(detalle.getMonto());
        dto.setValorResultado(detalle.getValorResultado());
        dto.setUnidad(detalle.getUnidad());
        dto.setRangoReferencia(detalle.getRangoReferencia());
        dto.setFechaResultado(detalle.getFechaResultado());
        dto.setFueraDeRango(detalle.isFueraDeRango());
        dto.setNotasResultado(detalle.getNotasResultado());
        dto.setPublicado(detalle.isPublicado());
        dto.setMensaje(mensaje);
        return dto;
    }
}