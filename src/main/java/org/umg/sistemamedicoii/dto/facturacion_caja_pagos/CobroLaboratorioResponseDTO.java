package org.umg.sistemamedicoii.dto.facturacion_caja_pagos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CobroLaboratorioResponseDTO {
    private String numeroTransaccion;
    private Integer ordenId;
    private String pacienteNombre;
    private BigDecimal monto;
    private String metodoPago;
    private BigDecimal montoRecibido;
    private BigDecimal cambio;
    private String mensaje;
}