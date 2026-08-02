package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaVigenteResponseDTO;
import org.umg.sistemamedicoii.service.DespachoFarmaciaService;

import java.util.List;

@RestController
@RequestMapping("/api/farmacia")
public class DespachoFarmaciaController {

    @Autowired
    private DespachoFarmaciaService despachoFarmaciaService;

    @GetMapping("/recetas/buscar")
    public List<RecetaVigenteResponseDTO> buscarRecetas(
            @RequestParam(required = false) Integer recetaId,
            @RequestParam(required = false) String dpi,
            @RequestParam(required = false) Integer consultaId) {
        // FIX CU-11: soporte de búsqueda por ID de Consulta, como pide el flujo normal del spec
        return despachoFarmaciaService.buscarRecetasVigentes(recetaId, dpi, consultaId);
    }

    @GetMapping("/recetas/{id}/detalle")
    public RecetaMedicaResponseDTO obtenerDetalle(@PathVariable Integer id) {
        return despachoFarmaciaService.obtenerDetalle(id);
    }

    @PostMapping("/despacho")
    public DespachoFarmaciaResponseDTO despachar(@Valid @RequestBody DespachoFarmaciaRequestDTO dto) {
        return despachoFarmaciaService.despachar(dto);
    }

    @PostMapping("/recetas/{id}/rechazar")
    public java.util.Map<String, String> rechazarReceta(@PathVariable Integer id) {
        // FIX CU-11 FA03: se devuelve el mensaje real construido en el service
        // (antes se descartaba y se mostraba un texto genérico fijo)
        String mensaje = despachoFarmaciaService.rechazarReceta(id);
        return java.util.Map.of("mensaje", mensaje);
    }
}