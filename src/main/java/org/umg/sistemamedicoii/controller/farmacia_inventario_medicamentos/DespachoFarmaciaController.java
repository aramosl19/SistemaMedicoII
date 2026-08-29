package org.umg.sistemamedicoii.controller.farmacia_inventario_medicamentos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.RecetaVigenteResponseDTO;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.DespachoFarmaciaService;

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
        return despachoFarmaciaService.buscarRecetasVigentes(recetaId, dpi, consultaId);
    }

    @GetMapping("/recetas/{id}/detalle")
    public RecetaMedicaResponseDTO obtenerDetalle(@PathVariable Integer id) {
        return despachoFarmaciaService.obtenerDetalle(id);
    }

    @Auditable(value = "Despachó medicamento(s)", entidad = "RECETA_MEDICA")
    @PostMapping("/despacho")
    public DespachoFarmaciaResponseDTO despachar(@Valid @RequestBody DespachoFarmaciaRequestDTO dto) {
        return despachoFarmaciaService.despachar(dto);
    }

    @Auditable(value = "Rechazó receta médica", entidad = "RECETA_MEDICA")
    @PostMapping("/recetas/{id}/rechazar")
    public java.util.Map<String, String> rechazarReceta(@PathVariable Integer id) {
        String mensaje = despachoFarmaciaService.rechazarReceta(id);
        return java.util.Map.of("mensaje", mensaje);
    }
}