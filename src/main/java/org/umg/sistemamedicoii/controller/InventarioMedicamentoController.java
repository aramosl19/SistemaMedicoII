package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.InventarioMedicamentoResponseDTO;
import org.umg.sistemamedicoii.service.InventarioMedicamentoService;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioMedicamentoController {

    @Autowired
    private InventarioMedicamentoService inventarioService;

    @GetMapping
    public List<InventarioMedicamentoResponseDTO> listar(@RequestParam(required = false) Integer sucursalId) {
        if (sucursalId != null) {
            return inventarioService.listarInventarioPorSucursal(sucursalId);
        }
        return inventarioService.listarTodo();
    }
}