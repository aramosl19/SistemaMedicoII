package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.service.MedicamentoServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoController {
    @Autowired
    private MedicamentoServiceImpl medicamentoService;

    @GetMapping
    public List<Medicamento> listar() {
        return medicamentoService.listar();
    }

    @GetMapping("/{id}")
    public Medicamento obtenerPorId(@PathVariable Integer id) {
        return medicamentoService.obtenerPorId(id);
    }

    @PostMapping
    public Medicamento crear(@RequestBody Medicamento medicamento) {
        return medicamentoService.crear(medicamento);
    }

    @PutMapping("/{id}")
    public Medicamento actualizar(@PathVariable Integer id,@RequestBody Medicamento medicamento) {
        return medicamentoService.actualizar(id,medicamento);
    }

    @DeleteMapping ("/{id}")
    public void eliminar(@PathVariable Integer id) {
        medicamentoService.eliminar(id);
    }
}
