package org.umg.sistemamedicoii.controller.farmacia_inventario_medicamentos;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.MovimientoInventarioResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.ResumenMensualInventarioResponseDTO;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.MovimientoInventarioService;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/movimientos")
public class MovimientoInventarioController {

    @Autowired
    private MovimientoInventarioService movimientoService;

    @GetMapping
    public List<MovimientoInventarioResponseDTO> listar() {
        return movimientoService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponseDTO registrarMovimiento(@Valid @RequestBody MovimientoInventarioRequestDTO dto) {
        return movimientoService.registrarMovimiento(dto);
    }

    @GetMapping("/resumen")
    public List<ResumenMensualInventarioResponseDTO> generarResumenMensual(
            @RequestParam Integer sucursalId,
            @RequestParam int anio,
            @RequestParam int mes) {

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        }

        return movimientoService.generarResumenMensual(sucursalId, anio, mes);
    }

    // Solución CU-15 (gap #2 del QA): endpoint que soporta el botón "Desactivar/Activar" de la tabla
    @PutMapping("/{id}/estado")
    public MovimientoInventarioResponseDTO toggleEstado(@PathVariable Integer id) {
        return movimientoService.toggleEstado(id);
    }
}