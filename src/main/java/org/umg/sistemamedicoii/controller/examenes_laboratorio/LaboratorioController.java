package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.examenes_laboratorio.Laboratorio;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
public class LaboratorioController {

    @Autowired
    private CatalogoService<Laboratorio> laboratorioService;

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
