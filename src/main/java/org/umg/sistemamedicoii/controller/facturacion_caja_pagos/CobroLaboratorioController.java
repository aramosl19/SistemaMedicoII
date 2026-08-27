package org.umg.sistemamedicoii.controller.facturacion_caja_pagos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.CobroLaboratorioService;

@RestController
@RequestMapping("/api/caja/laboratorio")
public class CobroLaboratorioController {

    @Autowired
    private CobroLaboratorioService cobroLaboratorioService;

    @PostMapping("/cobro")
    public CobroLaboratorioResponseDTO cobrar(@Valid @RequestBody CobroLaboratorioRequestDTO dto) {
        return cobroLaboratorioService.cobrar(dto);
    }

    @GetMapping("/ordenes/buscar")
    public java.util.List<OrdenLaboratorioResponseDTO> buscarOrdenes(
            @RequestParam(required = false) Integer numeroOrden,
            @RequestParam(required = false) String dpi) {
        return cobroLaboratorioService.buscarOrdenesPendientes(numeroOrden, dpi);
    }
}