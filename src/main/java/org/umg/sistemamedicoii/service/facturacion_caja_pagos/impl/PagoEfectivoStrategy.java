package org.umg.sistemamedicoii.service.facturacion_caja_pagos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.DatosCobroRequestDTO;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.PagoEfectivo;
import org.umg.sistemamedicoii.repository.facturacion_caja_pagos.PagoEfectivoRepository;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.ProcesadorPagoStrategy;

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
    public BigDecimal[] procesarPago(DatosCobroRequestDTO dto, BigDecimal montoACobrar, Integer referenciaId,
                                     String nombreTitular, String numeroTransaccion, TipoConceptoCobro tipoConcepto) {
        if (dto.getMontoRecibido() == null) {
            throw new IllegalArgumentException("Debe ingresar el monto recibido.");
        }
        if (dto.getMontoRecibido().compareTo(montoACobrar) < 0) {
            throw new IllegalArgumentException("El monto recibido (Q" + dto.getMontoRecibido() + ") es menor al monto a cobrar (Q" + montoACobrar + ")");
        }

        BigDecimal cambio = dto.getMontoRecibido().subtract(montoACobrar);

        PagoEfectivo pago = new PagoEfectivo();
        pago.setTipoConcepto(tipoConcepto);
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