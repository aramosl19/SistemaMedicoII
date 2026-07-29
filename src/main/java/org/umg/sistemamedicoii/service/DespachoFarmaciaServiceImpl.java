package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.*;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.DetalleReceta;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.models.RecetaMedica;
import org.umg.sistemamedicoii.repository.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.RecetaMedicaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DespachoFarmaciaServiceImpl implements DespachoFarmaciaService {

    private static final int VIGENCIA_DIAS = 7;

    @Autowired private org.umg.sistemamedicoii.repository.InventarioMedicamentoRepository inventarioRepo;
    @Autowired private org.umg.sistemamedicoii.repository.AuditoriaRepository auditoriaRepo;
    @Autowired private RecetaMedicaRepository recetaMedicaRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;
    @Autowired private List<ProcesadorPagoStrategy> estrategiasPago;

    @Override
    public List<RecetaVigenteResponseDTO> buscarRecetasVigentes(String dpi) {
        return recetaMedicaRepository.findByCita_Paciente_DpiAndActivoTrueOrderByFechaEmisionDesc(dpi).stream()
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

        BigDecimal montoTotal = BigDecimal.ZERO;
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
            org.umg.sistemamedicoii.models.InventarioMedicamento inv = inventarioRepo
                    .findByMedicamentoIdAndSucursalId(medicamentoDespachar.getId(), sucursalId)
                    .orElseThrow(() -> new IllegalArgumentException("Sin inventario registrado para " + medicamentoDespachar.getNombre() + " en esta sucursal."));

            if (inv.getStockActual() < itemReq.getCantidadDespachada()) {
                throw new IllegalArgumentException("Stock insuficiente de " + medicamentoDespachar.getNombre());
            }

            montoTotal = montoTotal.add(medicamentoDespachar.getPrecio().multiply(BigDecimal.valueOf(itemReq.getCantidadDespachada())));
        }

        // 2. Cobro (Patrón Strategy intacto)
        ProcesadorPagoStrategy estrategia = estrategiasPago.stream()
                .filter(e -> e.soportaMetodo(dto.getMetodoPago()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Método de pago no válido."));

        String numeroTransaccion = UUID.randomUUID().toString();
        BigDecimal[] montos = estrategia.procesarPago(dto, montoTotal, receta.getId(), receta.getCita().getPaciente().getNombreCompleto(), numeroTransaccion, TipoConceptoCobro.FARMACIA);

        // 3. Descontar Stock (por sucursal) y Auditoría real de controlados (RN-CU10-03 y RNF-017)
        for (ItemDespachoRequestDTO itemReq : dto.getItems()) {
            Medicamento med = itemReq.getMedicamentoSustitutoId() != null
                    ? medicamentoRepository.findById(itemReq.getMedicamentoSustitutoId()).get()
                    : receta.getDetalles().stream().filter(d -> d.getId().equals(itemReq.getDetalleRecetaId())).findFirst().get().getMedicamento();

            org.umg.sistemamedicoii.models.InventarioMedicamento inv = inventarioRepo
                    .findByMedicamentoIdAndSucursalId(med.getId(), sucursalId).get();
            inv.setStockActual(inv.getStockActual() - itemReq.getCantidadDespachada());
            inventarioRepo.save(inv);

            if (med.getMinimumStock() != null && inv.getStockActual() <= med.getMinimumStock()) {
                alertasStock.add("ALERTA: El medicamento " + med.getNombre() + " alcanzó stock mínimo.");
            }

            if (med.isControlled()) {
                org.umg.sistemamedicoii.models.Auditoria log = new org.umg.sistemamedicoii.models.Auditoria();
                log.setAccion("DESPACHO_CONTROLADO");
                log.setEntidadAfectada("MEDICAMENTO");
                log.setEntidadId(med.getId());
                log.setDetalle("Despachado al paciente DPI: " + receta.getCita().getPaciente().getDpi() + ", Cantidad: " + itemReq.getCantidadDespachada());
                log.setUsuarioEjecutorId(null); // Pendiente Security
                log.setFechaHora(LocalDateTime.now());
                auditoriaRepo.save(log);
            }
        }

        receta.setActivo(false); // Cierra la receta
        receta.setNotas(notasSustitucion.toString());
        recetaMedicaRepository.save(receta);

        DespachoFarmaciaResponseDTO respuesta = new DespachoFarmaciaResponseDTO();
        respuesta.setNumeroTransaccion(numeroTransaccion);
        respuesta.setMonto(montoTotal);
        respuesta.setMetodoPago(dto.getMetodoPago());
        respuesta.setMontoRecibido(montos[0]);
        respuesta.setCambio(montos[1]);
        respuesta.setAlertasStock(alertasStock);
        respuesta.setMensaje("Despacho registrado exitosamente. Total: Q" + montoTotal);

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
    public void rechazarReceta(Integer recetaId) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la receta."));

        if (!receta.isActivo()) {
            throw new IllegalArgumentException("La receta ya fue procesada o anulada.");
        }

        // Cerramos la receta y registramos la nota exacta que pide el documento
        receta.setActivo(false);
        receta.setNotas((receta.getNotas() != null ? receta.getNotas() + "\n" : "") +
                "Se ha registrado que el paciente " + receta.getCita().getPaciente().getNombreCompleto() +
                " no adquirió los medicamentos recetados en farmacia interna. Receta: " + receta.getId());

        recetaMedicaRepository.save(receta);
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