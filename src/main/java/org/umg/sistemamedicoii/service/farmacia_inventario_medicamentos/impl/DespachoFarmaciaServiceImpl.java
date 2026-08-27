package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.DetalleRecetaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.ItemDespachoRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.RecetaVigenteResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.DetalleReceta;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.InventarioMedicamento;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.Medicamento;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.RecetaMedica;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.MovimientoInventario;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.atencion_medica_enfermeria.RecetaMedicaRepository;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.InventarioMedicamentoRepository;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.MovimientoInventarioRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.DespachoFarmaciaService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DespachoFarmaciaServiceImpl implements DespachoFarmaciaService {

    private static final int VIGENCIA_DIAS = 7;

    @Autowired private InventarioMedicamentoRepository inventarioRepo;
    @Autowired private AuditoriaRepository auditoriaRepo;
    @Autowired private MovimientoInventarioRepository movimientoInventarioRepo;
    @Autowired private RecetaMedicaRepository recetaMedicaRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;

    private static final int TIPO_MOVIMIENTO_DESPACHO = 6;

    @Override
    public List<RecetaVigenteResponseDTO> buscarRecetasVigentes(Integer recetaId, String dpi, Integer consultaId) {
        // FIX CU-11: se agrega consultaId como tercer criterio válido de búsqueda
        if (recetaId == null && (dpi == null || dpi.isBlank()) && consultaId == null) {
            throw new IllegalArgumentException("Debe ingresar un ID de receta, ID de consulta o DPI para buscar.");
        }

        List<RecetaMedica> recetas = new ArrayList<>();
        if (recetaId != null) {
            recetaMedicaRepository.findById(recetaId)
                    .filter(RecetaMedica::isActivo)
                    .ifPresent(recetas::add);
        } else if (consultaId != null) {
            recetas = recetaMedicaRepository.findByCita_IdAndActivoTrue(consultaId);
        } else {
            recetas = recetaMedicaRepository.findByCita_Paciente_DpiAndActivoTrueOrderByFechaEmisionDesc(dpi);
        }

        return recetas.stream()
                .filter(this::esVigente)
                .map(this::toVigenteDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DespachoFarmaciaResponseDTO despachar(DespachoFarmaciaRequestDTO dto) {
        RecetaMedica receta = recetaMedicaRepository.findById(dto.getRecetaId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta ingresada."));

        if (!receta.isActivo() || !esVigente(receta)) {
            throw new IllegalArgumentException("La receta es inválida o está vencida (máximo 7 días).");
        }

        List<String> alertasStock = new ArrayList<>();
        StringBuilder notasSustitucion = new StringBuilder(receta.getNotas() != null ? receta.getNotas() : "");

        Integer sucursalId = receta.getCita().getSucursal().getId();

        // 1. Validar Stock e Items (por sucursal)
        for (ItemDespachoRequestDTO itemReq : dto.getItems()) {
            DetalleReceta detalleOriginal = receta.getDetalles().stream()
                    .filter(d -> d.getId().equals(itemReq.getDetalleRecetaId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El detalle de receta no pertenece a esta orden."));

            Medicamento medicamentoDespachar;

            if (itemReq.getMedicamentoSustitutoId() != null) {
                if (itemReq.getRazonSustitucion() == null || itemReq.getRazonSustitucion().isBlank()) {
                    throw new IllegalArgumentException("Debe ingresar la razón de la sustitución.");
                }
                medicamentoDespachar = medicamentoRepository.findById(itemReq.getMedicamentoSustitutoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Medicamento sustituto no encontrado."));

                notasSustitucion.append(String.format(" | Sustitución: %s por %s. Razón: %s",
                        detalleOriginal.getMedicamento().getNombre(), medicamentoDespachar.getNombre(), itemReq.getRazonSustitucion()));
            } else {
                medicamentoDespachar = detalleOriginal.getMedicamento();
            }

            // Consultar inventario por sucursal
            InventarioMedicamento inv = inventarioRepo
                    .findByMedicamentoIdAndSucursalId(medicamentoDespachar.getId(), sucursalId)
                    .orElseThrow(() -> new IllegalArgumentException("Sin inventario registrado para " + medicamentoDespachar.getNombre() + " en esta sucursal."));

            if (inv.getStockActual() < itemReq.getCantidadDespachada()) {
                throw new IllegalArgumentException("Stock insuficiente de " + medicamentoDespachar.getNombre());
            }
        }

        // Solución CU-11: El Cobro queda totalmente fuera de esta capa.
        String numeroTransaccion = UUID.randomUUID().toString(); // Referencia del despacho

        // FIX CU-11: acumuladores para el mensaje de éxito con cantidad y monto (spec)
        int totalDespachado = 0;
        BigDecimal totalMonto = BigDecimal.ZERO;

        // 3. Descontar Stock (por sucursal) y Auditoría real de controlados (RN-CU10-03 y RNF-017)
        for (ItemDespachoRequestDTO itemReq : dto.getItems()) {
            Medicamento med = itemReq.getMedicamentoSustitutoId() != null
                    ? medicamentoRepository.findById(itemReq.getMedicamentoSustitutoId()).get()
                    : receta.getDetalles().stream().filter(d -> d.getId().equals(itemReq.getDetalleRecetaId())).findFirst().get().getMedicamento();

            InventarioMedicamento inv = inventarioRepo
                    .findByMedicamentoIdAndSucursalId(med.getId(), sucursalId).get();
            int stockAnterior = inv.getStockActual();
            inv.setStockActual(stockAnterior - itemReq.getCantidadDespachada());
            inventarioRepo.save(inv);

            // RN-CU13-01/CU-15: el tipo "Despacho (6)" es automático y lo genera este módulo;
            // sin este registro, las ventas hechas en farmacia interna no quedaban reflejadas
            // en la bitácora de movimientos ni en su resumen mensual.
            MovimientoInventario movimiento = new MovimientoInventario();
            movimiento.setTipoMovimiento(TIPO_MOVIMIENTO_DESPACHO);
            movimiento.setMedicamento(med);
            movimiento.setSucursal(receta.getCita().getSucursal());
            movimiento.setCantidad(itemReq.getCantidadDespachada());
            movimiento.setStockAnterior(stockAnterior);
            movimiento.setStockNuevo(inv.getStockActual());
            movimiento.setCostoUnitario(med.getPrecio());
            movimiento.setReferencia("Receta #" + receta.getId());
            movimiento.setActivo(true);
            movimiento.setFechaHora(LocalDateTime.now());
            var authMov = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authMov != null && authMov.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principalMov) {
                movimiento.setUsuarioId(principalMov.getUsuario().getId());
            }
            movimientoInventarioRepo.save(movimiento);

            // FIX CU-11: acumular cantidad y monto de este item
            totalDespachado += itemReq.getCantidadDespachada();
            if (med.getPrecio() != null) {
                totalMonto = totalMonto.add(med.getPrecio().multiply(BigDecimal.valueOf(itemReq.getCantidadDespachada())));
            }

            if (med.getMinimumStock() != null && inv.getStockActual() <= med.getMinimumStock()) {
                alertasStock.add("ALERTA: El medicamento " + med.getNombre() + " alcanzó stock mínimo.");
            }

            if (med.isControlled()) {
                Auditoria log = new Auditoria();
                log.setAccion("DESPACHO_CONTROLADO");
                log.setEntidadAfectada("MEDICAMENTO");
                log.setEntidadId(med.getId());
                log.setDetalle("Despachado al paciente DPI: " + receta.getCita().getPaciente().getDpi() + ", Cantidad: " + itemReq.getCantidadDespachada());

                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
                    log.setUsuarioEjecutorId(principal.getUsuario().getId());
                } else {
                    log.setUsuarioEjecutorId(null);
                }

                log.setFechaHora(LocalDateTime.now());
                auditoriaRepo.save(log);
            }
        }

        receta.setActivo(false); // Cierra la receta
        receta.setNotas(notasSustitucion.toString());
        recetaMedicaRepository.save(receta);

        DespachoFarmaciaResponseDTO respuesta = new DespachoFarmaciaResponseDTO();
        respuesta.setNumeroTransaccion(numeroTransaccion);
        respuesta.setAlertasStock(alertasStock);
        // FIX CU-11: mensaje alineado al spec, con cantidad y monto total despachado
        respuesta.setMensaje("Despacho registrado exitosamente. " + totalDespachado
                + " medicamento(s) despachado(s). Total: Q" + totalMonto.setScale(2, RoundingMode.HALF_UP) + ".");

        List<DetalleRecetaResponseDTO> detallesRespuesta = new ArrayList<>();
        for (ItemDespachoRequestDTO itemReq : dto.getItems()) {
            Medicamento med = itemReq.getMedicamentoSustitutoId() != null
                    ? medicamentoRepository.findById(itemReq.getMedicamentoSustitutoId()).get()
                    : receta.getDetalles().stream().filter(d -> d.getId().equals(itemReq.getDetalleRecetaId())).findFirst().get().getMedicamento();

            DetalleRecetaResponseDTO det = new DetalleRecetaResponseDTO();
            det.setId(itemReq.getDetalleRecetaId());
            det.setMedicamentoNombre(med.getNombre());
            det.setCantidad(itemReq.getCantidadDespachada());
            det.setPrecioUnitario(med.getPrecio());
            det.setSubtotal(med.getPrecio().multiply(BigDecimal.valueOf(itemReq.getCantidadDespachada())));
            detallesRespuesta.add(det);
        }
        respuesta.setMedicamentosDespachados(detallesRespuesta);
        return respuesta;
    }

    @Override
    public String rechazarReceta(Integer recetaId) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta."));

        if (!receta.isActivo()) {
            throw new IllegalArgumentException("La receta ya fue procesada o anulada.");
        }

        // FIX CU-11 FA03: el mensaje ahora se devuelve al controller en vez de
        // quedar solo en las notas de auditoría (antes el usuario veía un texto genérico fijo)
        String mensaje = "Se ha registrado que el paciente " + receta.getCita().getPaciente().getNombreCompleto() +
                " no adquirió los medicamentos recetados en farmacia interna. Receta: " + receta.getId();

        receta.setActivo(false);
        receta.setNotas((receta.getNotas() != null ? receta.getNotas() + "\n" : "") + mensaje);

        recetaMedicaRepository.save(receta);
        return mensaje;
    }

    @Override
    public RecetaMedicaResponseDTO obtenerDetalle(Integer recetaId) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta ingresada."));

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
            det.setSubtotal(d.getMedicamento().getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())));
            return det;
        }).collect(Collectors.toList()));
        return dto;
    }

    private boolean esVigente(RecetaMedica receta) {
        return receta.isActivo() && receta.getFechaEmision().plusDays(VIGENCIA_DIAS).isAfter(LocalDateTime.now());
    }

    private RecetaVigenteResponseDTO toVigenteDTO(RecetaMedica receta) {
        RecetaVigenteResponseDTO dto = new RecetaVigenteResponseDTO();
        dto.setId(receta.getId());
        dto.setPacienteNombre(receta.getCita().getPaciente().getNombreCompleto());
        dto.setMedicoNombre(receta.getMedico().getNombreCompleto());
        dto.setFechaEmision(receta.getFechaEmision());
        dto.setCantidadMedicamentos(receta.getDetalles().size());
        return dto;

    }
}