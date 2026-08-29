package org.umg.sistemamedicoii.controller.farmacia_inventario_medicamentos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoRequestDTO;
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

    @Auditable(value = "Creó inventario de medicamento por sede", entidad = "INVENTARIO_MEDICAMENTO")
    @PostMapping
    public InventarioMedicamentoResponseDTO crear(@Valid @RequestBody InventarioMedicamentoRequestDTO dto) {
        return inventarioService.crear(dto);
    }

    @Auditable(value = "Actualizó inventario de medicamento por sede", entidad = "INVENTARIO_MEDICAMENTO")
    @PutMapping("/{id}")
    public InventarioMedicamentoResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody InventarioMedicamentoRequestDTO dto) {
        return inventarioService.actualizar(id, dto);
    }

    @Auditable(value = "Eliminó inventario de medicamento por sede", entidad = "INVENTARIO_MEDICAMENTO")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        inventarioService.eliminar(id);
    }
}