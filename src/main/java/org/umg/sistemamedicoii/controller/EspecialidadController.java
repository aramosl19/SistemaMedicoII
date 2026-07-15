package org.umg.sistemamedicoii.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.Especialidad;
import org.umg.sistemamedicoii.service.EspecialidadServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    @Autowired
    private EspecialidadServiceImpl especialidadService;

    @GetMapping
    public List<Especialidad> listar() {
        return especialidadService.listar();
    }

    @GetMapping("/{id}")
    public Especialidad obtenerPorId(@PathVariable Integer id) {
        return especialidadService.obtenerPorId(id);
    }

    @PostMapping
    public Especialidad crear(@RequestBody Especialidad especialidad) {
        return especialidadService.crear(especialidad);
    }

    @PutMapping("/{id}")
    public Especialidad actualizar(@PathVariable Integer id, @RequestBody Especialidad especialidad){
        return  especialidadService.actualizar(id, especialidad);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        especialidadService.eliminar(id);
    }
}
