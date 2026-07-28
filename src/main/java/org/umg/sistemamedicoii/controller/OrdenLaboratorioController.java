package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.service.OrdenLaboratorioService;

@RestController
@RequestMapping("/api/medico")
public class OrdenLaboratorioController {

    @Autowired
    private OrdenLaboratorioService ordenLaboratorioService;

    @PostMapping("/citas/{id}/orden-laboratorio")
    public OrdenLaboratorioResponseDTO generarOrden(
            @PathVariable Integer id,
            @RequestBody OrdenLaboratorioRequestDTO dto) {
        return ordenLaboratorioService.generarOrden(id, dto);
    }
}