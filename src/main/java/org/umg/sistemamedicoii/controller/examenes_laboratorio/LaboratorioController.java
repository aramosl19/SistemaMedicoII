package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.LaboratorioRequestDTO;
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

    @Auditable(value = "Creó Laboratorio", entidad = "LABORATORIO")
    @PostMapping
    public Laboratorio crear(@Valid @RequestBody LaboratorioRequestDTO dto) {
        return laboratorioService.crear(mapToEntity(new Laboratorio(), dto));
    }

    @Auditable(value = "Actualizó Laboratorio", entidad = "LABORATORIO")
    @PutMapping("/{id}")
    public Laboratorio actualizar(@PathVariable Integer id, @Valid @RequestBody LaboratorioRequestDTO dto){
        return  laboratorioService.actualizar(id, mapToEntity(new Laboratorio(), dto));
    }

    @Auditable(value = "Eliminó Laboratorio", entidad = "LABORATORIO")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        laboratorioService.eliminar(id);
    }

    private Laboratorio mapToEntity(Laboratorio l, LaboratorioRequestDTO dto) {
        l.setNombre(dto.getNombre());
        l.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) l.setActivo(dto.getActivo());
        return l;
    }
}