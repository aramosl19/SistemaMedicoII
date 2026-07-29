package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.MovimientoInventarioResponseDTO;
import org.umg.sistemamedicoii.service.MovimientoInventarioService;

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
}