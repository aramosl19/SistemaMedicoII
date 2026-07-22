package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.PagoResponseDTO;
import org.umg.sistemamedicoii.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @PostMapping
    public PagoResponseDTO pagar(@Valid @RequestBody PagoRequestDTO dto) {
        return pagoService.procesarPago(dto);
    }
}