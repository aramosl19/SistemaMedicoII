package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.Laboratorio;
import org.umg.sistemamedicoii.service.LaboratorioServiceImpl;

import java.util.List;

@RestController
@RequestMapping
public class LaboratorioController {

    @Autowired
    private LaboratorioServiceImpl laboratorioService;

    @GetMapping
    public List<Laboratorio> listar() {
        return laboratorioService.listar();
    }

    @GetMapping("/{id}")
    public Laboratorio obtenerPorId(@PathVariable Integer id) {
        return  laboratorioService.obtenerPorId(id);
    }

    @PostMapping
    public Laboratorio crear(@RequestBody Laboratorio laboratorio) {
        return laboratorioService.crear(laboratorio);
    }

    @PutMapping("/{id}")
    public Laboratorio actualizar(@PathVariable Integer id, @RequestBody Laboratorio laboratorio){
        return  laboratorioService.actualizar(id,laboratorio);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        laboratorioService.eliminar(id);
    }

}
