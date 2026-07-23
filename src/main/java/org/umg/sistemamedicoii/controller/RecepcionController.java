package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.RegistrarLlegadaRequestDTO;
import org.umg.sistemamedicoii.service.RecepcionService;

@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    @Autowired
    private RecepcionService recepcionService;

    @GetMapping("/citas/buscar")
    public BusquedaRecepcionResponseDTO buscar(
            @RequestParam(required = false) Integer numeroCita,
            @RequestParam(required = false) String dpi) {
        return recepcionService.buscar(numeroCita, dpi);
    }

    @PostMapping("/citas/{id}/llegada")
    public CitaRecepcionResponseDTO registrarLlegada(
            @PathVariable Integer id,
            @RequestBody(required = false) RegistrarLlegadaRequestDTO dto) {
        boolean esEmergencia = dto != null && dto.isEmergencia();
        return recepcionService.registrarLlegada(id, esEmergencia);
    }
}