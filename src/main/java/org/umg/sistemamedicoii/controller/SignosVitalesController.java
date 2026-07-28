package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.CitaEnfermeriaResponseDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesRequestDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesResponseDTO;
import org.umg.sistemamedicoii.service.SignosVitalesService;

@RestController
@RequestMapping("/api/enfermeria")
public class SignosVitalesController {

    @Autowired
    private SignosVitalesService signosVitalesService;

    @PostMapping("/citas/{id}/llamar")
    public CitaEnfermeriaResponseDTO llamarPaciente(@PathVariable Integer id) {
        return signosVitalesService.llamarPaciente(id);
    }

    @PostMapping("/citas/{id}/signos-vitales")
    public SignosVitalesResponseDTO registrarSignosVitales(
            @PathVariable Integer id,
            @RequestBody SignosVitalesRequestDTO dto) {
        return signosVitalesService.registrarSignosVitales(id, dto);
    }
}