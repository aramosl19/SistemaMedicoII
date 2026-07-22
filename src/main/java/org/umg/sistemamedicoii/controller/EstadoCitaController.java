package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.EstadoCita;
import org.umg.sistemamedicoii.service.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/estados-cita")
public class EstadoCitaController {

    @Autowired
    private CatalogoService<EstadoCita> estadoCitaService;

    @GetMapping
    public List<EstadoCita> listar() {
        return estadoCitaService.listar();
    }

    @GetMapping("/{id}")
    public EstadoCita obtenerPorId(@PathVariable Integer id) {
        return estadoCitaService.obtenerPorId(id);
    }

    @PostMapping
    public EstadoCita crear(@Valid @RequestBody EstadoCita entidad) {
        return estadoCitaService.crear(entidad);
    }

    @PutMapping("/{id}")
    public EstadoCita actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoCita entidad) {
        return estadoCitaService.actualizar(id, entidad);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        estadoCitaService.eliminar(id);
    }
}