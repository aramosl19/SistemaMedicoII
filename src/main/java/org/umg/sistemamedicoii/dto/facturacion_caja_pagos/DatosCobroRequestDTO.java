package org.umg.sistemamedicoii.dto.facturacion_caja_pagos;

import java.math.BigDecimal;

public interface DatosCobroRequestDTO {
    String getMetodoPago();
    BigDecimal getMontoRecibido();
    String getUltimosCuatroDigitos();
}