package org.umg.sistemamedicoii.controller.facturacion_caja_pagos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoResponseDTO;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;
    
    @PostMapping
    public PagoResponseDTO pagar(
            @Valid @RequestBody PagoRequestDTO dto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return pagoService.procesarPago(dto, idempotencyKey);
    }
}