package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import java.math.BigDecimal;

public interface ProcesadorPagoStrategy {
    boolean soportaMetodo(String metodoPago);

    BigDecimal[] procesarPago(CobroCajaRequestDTO dto, BigDecimal montoACobrar, Integer referenciaId, String nombreTitular, String numeroTransaccion);
}