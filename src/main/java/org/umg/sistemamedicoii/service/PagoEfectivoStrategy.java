package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.models.PagoEfectivo;
import org.umg.sistemamedicoii.repository.PagoEfectivoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class PagoEfectivoStrategy implements ProcesadorPagoStrategy {

    @Autowired
    private PagoEfectivoRepository pagoEfectivoRepository;

    @Override
    public boolean soportaMetodo(String metodoPago) {
        return "EFECTIVO".equalsIgnoreCase(metodoPago.trim());
    }

    @Override
    public BigDecimal[] procesarPago(CobroCajaRequestDTO dto, BigDecimal montoACobrar, Integer referenciaId, String nombreTitular, String numeroTransaccion) {
        if (dto.getMontoRecibido() == null) {
            throw new IllegalArgumentException("Debe ingresar el monto recibido.");
        }
        if (dto.getMontoRecibido().compareTo(montoACobrar) < 0) {
            throw new IllegalArgumentException("El monto recibido (Q" + dto.getMontoRecibido() + ") es menor al monto a cobrar (Q" + montoACobrar + ")");
        }

        BigDecimal cambio = dto.getMontoRecibido().subtract(montoACobrar);

        PagoEfectivo pago = new PagoEfectivo();
        pago.setTipoConcepto(TipoConceptoCobro.CITA);
        pago.setReferenciaId(referenciaId);
        pago.setNumeroTransaccion(numeroTransaccion);
        pago.setMonto(montoACobrar);
        pago.setMontoRecibido(dto.getMontoRecibido());
        pago.setCambio(cambio);
        pago.setFechaPago(LocalDateTime.now());

        pagoEfectivoRepository.save(pago);

        return new BigDecimal[]{dto.getMontoRecibido(), cambio};
    }
}