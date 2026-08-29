package org.umg.sistemamedicoii.controller.facturacion_caja_pagos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroCajaResponseDTO;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.CajaService;

import java.util.List;

@RestController
@RequestMapping("/api/caja")
public class CajaController {

    @Autowired
    private CajaService cajaService;

    @GetMapping("/citas/buscar")
    public List<CitaCobroResponseDTO> buscarPendientes(
            @RequestParam(required = false) Integer numeroCita,
            @RequestParam(required = false) String dpi,
            @AuthenticationPrincipal UsuarioPrincipal principal) {

        boolean esPaciente = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PACIENTE"));

        if (esPaciente) {
            return cajaService.buscarCitasPendientesPropias(principal.getUsuario().getId());
        }

        return cajaService.buscarCitasPendientes(numeroCita, dpi);
    }

    @Auditable(value = "Registró cobro en caja", entidad = "CITA")
    @PostMapping("/cobro")
    public CobroCajaResponseDTO cobrar(@Valid @RequestBody CobroCajaRequestDTO dto) {
        return cajaService.procesarCobro(dto);
    }
}