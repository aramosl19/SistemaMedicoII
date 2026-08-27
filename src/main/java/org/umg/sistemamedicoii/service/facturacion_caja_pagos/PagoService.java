package org.umg.sistemamedicoii.service.facturacion_caja_pagos;

import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoResponseDTO;

public interface PagoService {
    PagoResponseDTO procesarPago(PagoRequestDTO dto, String idempotencyKey);
}