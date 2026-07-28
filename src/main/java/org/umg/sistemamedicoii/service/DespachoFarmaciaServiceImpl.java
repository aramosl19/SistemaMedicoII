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

        // RN-CU10-01: Verificación de Receta
        if (!receta.isActivo() || !esVigente(receta)) {
            throw new IllegalArgumentException(
                    "La receta es inválida. Verifique que el medicamento exista, la dosis sea correcta y la receta no sea anterior a 7 días.");
        }

        BigDecimal montoTotal = BigDecimal.ZERO;
        List<String> alertasStock = new ArrayList<>();

        // Validamos stock de TODOS los medicamentos antes de descontar nada
        for (DetalleReceta detalle : receta.getDetalles()) {
            Medicamento medicamento = detalle.getMedicamento();
            if (medicamento.getStockActual() < detalle.getCantidad()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente del medicamento " + medicamento.getNombre()
                                + ". Disponible: " + medicamento.getStockActual() + ", solicitado: " + detalle.getCantidad() + ".");
            }
            montoTotal = montoTotal.add(medicamento.getPrecio().multiply(BigDecimal.valueOf(detalle.getCantidad())));
        }

        // RN-CU10-02: Cobro Integrado en Farmacia
        ProcesadorPagoStrategy estrategia = estrategiasPago.stream()
                .filter(e -> e.soportaMetodo(dto.getMetodoPago()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El método de pago seleccionado no está disponible. Los métodos aceptados son: efectivo (Quetzales), tarjeta de crédito (Visa/Mastercard) o tarjeta de débito."));

        String numeroTransaccion = UUID.randomUUID().toString();
        String nombreTitular = receta.getCita().getPaciente().getNombreCompleto();

        BigDecimal[] montos = estrategia.procesarPago(
                dto, montoTotal, receta.getId(), nombreTitular, numeroTransaccion, TipoConceptoCobro.FARMACIA);

        // Descontamos stock y evaluamos RN-CU10-03: Stock Mínimo
        for (DetalleReceta detalle : receta.getDetalles()) {
            Medicamento medicamento = detalle.getMedicamento();
            medicamento.setStockActual(medicamento.getStockActual() - detalle.getCantidad());
            medicamentoRepository.save(medicamento);

            if (medicamento.getMinimumStock() != null && medicamento.getStockActual() <= medicamento.getMinimumStock()) {
                alertasStock.add("El stock del medicamento " + medicamento.getNombre()
                        + " ha alcanzado el nivel mínimo. Se requiere reorden.");
            }
        }

        receta.setActivo(false); // la receta queda despachada ya no es "vigente"
        recetaMedicaRepository.save(receta);

        DespachoFarmaciaResponseDTO respuesta = new DespachoFarmaciaResponseDTO();
        respuesta.setNumeroTransaccion(numeroTransaccion);
        respuesta.setRecetaId(receta.getId());
        respuesta.setPacienteNombre(nombreTitular);
        respuesta.setSucursalNombre(receta.getCita().getSucursal().getNombre());
        respuesta.setMedicamentosDespachados(receta.getDetalles().stream().map(d -> {
            DetalleRecetaResponseDTO det = new DetalleRecetaResponseDTO();
            det.setId(d.getId());
            det.setMedicamentoNombre(d.getMedicamento().getNombre());
            det.setCantidad(d.getCantidad());
            det.setPrecioUnitario(d.getMedicamento().getPrecio());
            det.setSubtotal(d.getMedicamento().getPrecio().multiply(BigDecimal.valueOf(d.getCantidad())));
            return det;
        }).collect(Collectors.toList()));
        respuesta.setMonto(montoTotal);
        respuesta.setMetodoPago(dto.getMetodoPago());
        respuesta.setMontoRecibido(montos[0]);
        respuesta.setCambio(montos[1]);
        respuesta.setAlertasStock(alertasStock);
        respuesta.setMensaje("Despacho registrado exitosamente. " + receta.getDetalles().size()
                + " medicamento(s) despachado(s). Total: Q" + montoTotal + ".");
        return respuesta;
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