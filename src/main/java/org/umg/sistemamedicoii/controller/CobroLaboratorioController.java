package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.service.CobroLaboratorioService;

@RestController
@RequestMapping("/api/caja/laboratorio")
public class CobroLaboratorioController {

    @Autowired
    private CobroLaboratorioService cobroLaboratorioService;

    @PostMapping("/cobro")
    public CobroLaboratorioResponseDTO cobrar(@Valid @RequestBody CobroLaboratorioRequestDTO dto) {
        return cobroLaboratorioService.cobrar(dto);
    }
}