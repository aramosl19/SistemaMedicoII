// controller/DespachoFarmaciaController.java  (nuevo)
package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaVigenteResponseDTO;
import org.umg.sistemamedicoii.service.DespachoFarmaciaService;

import java.util.List;

@RestController
@RequestMapping("/api/farmacia")
public class DespachoFarmaciaController {

    @Autowired
    private DespachoFarmaciaService despachoFarmaciaService;

    @GetMapping("/recetas/buscar")
    public List<RecetaVigenteResponseDTO> buscarRecetas(@RequestParam String dpi) {
        return despachoFarmaciaService.buscarRecetasVigentes(dpi);
    }

    @PostMapping("/despacho")
    public DespachoFarmaciaResponseDTO despachar(@Valid @RequestBody DespachoFarmaciaRequestDTO dto) {
        return despachoFarmaciaService.despachar(dto);
    }
}