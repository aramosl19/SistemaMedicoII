package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.DatosCobroRequestDTO;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;

import java.math.BigDecimal;

public interface ProcesadorPagoStrategy {
    boolean soportaMetodo(String metodoPago);

    BigDecimal[] procesarPago(DatosCobroRequestDTO dto, BigDecimal montoACobrar, Integer referenciaId,
                              String nombreTitular, String numeroTransaccion, TipoConceptoCobro tipoConcepto);
}