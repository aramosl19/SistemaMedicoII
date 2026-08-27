package org.umg.sistemamedicoii.service.examenes_laboratorio.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.RegistrarResultadoRequestDTO;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.examenes_laboratorio.DetalleOrdenLaboratorio;
import org.umg.sistemamedicoii.models.examenes_laboratorio.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.DetalleOrdenLaboratorioRepository;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.OrdenLaboratorioRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;
import org.umg.sistemamedicoii.service.examenes_laboratorio.LaboratorioResultadoService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LaboratorioResultadoServiceImpl implements LaboratorioResultadoService {

    @Autowired private OrdenLaboratorioRepository ordenLaboratorioRepository;
    @Autowired private DetalleOrdenLaboratorioRepository detalleOrdenLaboratorioRepository;
    @Autowired private EmailService emailService;

    @Override
    public List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado) {
        return listarOrdenes(estado, null);
    }

    @Override
    public List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado, Integer medicoId) {
        EstadoOrdenLaboratorioEnum filtro = parsearEstado(estado);
        List<OrdenLaboratorio> ordenes = (medicoId != null)
                ? ordenLaboratorioRepository.findByEstadoAndMedico_IdOrderByFechaCreacionAsc(filtro, medicoId)
                : ordenLaboratorioRepository.findByEstadoOrderByFechaCreacionAsc(filtro);
        return ordenes.stream()
                .map(this::toOrdenResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        return toOrdenResponseDTO(orden);
    }

    @Override
    public OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId, Integer medicoId) {
        OrdenLaboratorio orden = buscarOrden(ordenId);
        // FIX QA (gap #2): un médico no debe poder consultar el detalle de la orden de otro médico
        if (medicoId != null && (orden.getMedico() == null || !medicoId.equals(orden.getMedico().getId()))) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permisos para consultar esta orden de laboratorio.");
        }
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

        // RN-CU09-02: el único campo del resultado que el documento exige de
        // verdad es el valor -- "todos los exámenes solicitados deben tener
        // resultados". Unidad y rango de referencia quedan opcionales, igual
        // que están marcados a nivel de catálogo en RN-CU15-03.
        if (dto.getValorResultado() == null || dto.getValorResultado().isBlank()) {
            throw new IllegalArgumentException("El valor del resultado es obligatorio.");
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

        // FIX QA (RNF-007): notificar al médico tratante que hay un resultado de laboratorio disponible.
        notificarResultadoAlMedico(orden, detalle);

        return toDetalleResponseDTO(detalle, "Resultado publicado exitosamente.");
    }

    private void notificarResultadoAlMedico(OrdenLaboratorio orden, DetalleOrdenLaboratorio detalle) {
        if (orden.getMedico() == null || orden.getMedico().getCorreo() == null || orden.getMedico().getCorreo().isBlank()) {
            return;
        }
        String asunto = "Resultado de Laboratorio Disponible - Orden #" + orden.getId();
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Estimado(a) Dr(a). ").append(orden.getMedico().getNombreCompleto()).append(",\n\n");
        mensaje.append("Se ha publicado un nuevo resultado de laboratorio para su paciente ")
                .append(orden.getCita().getPaciente().getNombreCompleto()).append(".\n\n");
        mensaje.append("Examen: ").append(detalle.getExamen().getNombre()).append("\n");
        mensaje.append("Resultado: ").append(detalle.getValorResultado())
                .append(detalle.getUnidad() != null ? " " + detalle.getUnidad() : "").append("\n");
        if (detalle.isFueraDeRango()) {
            mensaje.append("ALERTA: Este resultado está fuera del rango de referencia normal. Requiere revisión.\n");
        }
        mensaje.append("\nPuede consultar el detalle completo de la orden #").append(orden.getId())
                .append(" desde el sistema.\n\n");
        mensaje.append("Atentamente,\nSistema Informático Hospitalario");

        emailService.enviarCorreo(orden.getMedico().getCorreo(), asunto, mensaje.toString());
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