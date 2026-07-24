package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.CobroCajaResponseDTO;
import org.umg.sistemamedicoii.service.CajaService;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaService cajaService;

    @GetMapping("/citas/buscar")
    public List<CitaCobroResponseDTO> buscarPendientes(
            @RequestParam(required = false) Integer numeroCita,
            @RequestParam(required = false) String dpi) {
        return cajaService.buscarCitasPendientes(numeroCita, dpi);
    }

    @PostMapping("/cobro")
    public CobroCajaResponseDTO cobrar(@Valid @RequestBody CobroCajaRequestDTO dto) {
        return cajaService.procesarCobro(dto);
    }
}