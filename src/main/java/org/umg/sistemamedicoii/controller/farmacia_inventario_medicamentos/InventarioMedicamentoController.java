package org.umg.sistemamedicoii.controller.farmacia_inventario_medicamentos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoResponseDTO;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.InventarioMedicamentoService;

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