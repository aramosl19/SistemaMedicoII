package org.umg.sistemamedicoii.dto;

import java.math.BigDecimal;

public interface DatosCobroRequestDTO {
    String getMetodoPago();
    BigDecimal getMontoRecibido();
    String getUltimosCuatroDigitos();
}