package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.MedicamentoRequestDTO;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.service.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {

    @Autowired
    private CatalogoService<Medicamento> medicamentoService;

    @GetMapping
    public List<Medicamento> listar() {
        return medicamentoService.listar();
    }

    @GetMapping("/{id}")
    public Medicamento obtenerPorId(@PathVariable Integer id) {
        return medicamentoService.obtenerPorId(id);
    }

    @PostMapping
    public Medicamento crear(@Valid @RequestBody MedicamentoRequestDTO dto) {
        Medicamento med = mapToEntity(new Medicamento(), dto);
        return medicamentoService.crear(med);
    }

    @PutMapping("/{id}")
    public Medicamento actualizar(@PathVariable Integer id, @Valid @RequestBody MedicamentoRequestDTO dto) {
        Medicamento med = mapToEntity(new Medicamento(), dto);
        return medicamentoService.actualizar(id, med);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        medicamentoService.eliminar(id);
    }

    // Mapeo seguro en la capa REST para no romper el servicio genérico
    private Medicamento mapToEntity(Medicamento med, MedicamentoRequestDTO dto) {
        med.setNombre(dto.getNombre());
        med.setDescripcion(dto.getDescripcion());
        med.setPrecio(dto.getPrecio());
        med.setUnidad(dto.getUnidad());
        med.setControlled(dto.isControlled());
        med.setMinimumStock(dto.getMinimumStock());
        return med;
    }
}