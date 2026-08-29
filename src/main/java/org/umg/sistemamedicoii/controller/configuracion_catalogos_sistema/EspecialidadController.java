package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.EspecialidadRequestDTO;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    @Autowired
    private CatalogoService<Especialidad> especialidadService;

    @GetMapping
    public List<Especialidad> listar() {
        return especialidadService.listar();
    }

    @GetMapping("/{id}")
    public Especialidad obtenerPorId(@PathVariable Integer id) {
        return especialidadService.obtenerPorId(id);
    }

    @Auditable(value = "Creó Especialidad", entidad = "ESPECIALIDAD")
    @PostMapping
    public Especialidad crear(@Valid @RequestBody EspecialidadRequestDTO dto) {
        return especialidadService.crear(mapToEntity(new Especialidad(), dto));
    }

    @Auditable(value = "Actualizó Especialidad", entidad = "ESPECIALIDAD")
    @PutMapping("/{id}")
    public Especialidad actualizar(@PathVariable Integer id, @Valid @RequestBody EspecialidadRequestDTO dto){
        return especialidadService.actualizar(id, mapToEntity(new Especialidad(), dto));
    }

    @Auditable(value = "Eliminó Especialidad", entidad = "ESPECIALIDAD")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        especialidadService.eliminar(id);
    }

    private Especialidad mapToEntity(Especialidad e, EspecialidadRequestDTO dto) {
        e.setNombre(dto.getNombre());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) e.setActivo(dto.getActivo());
        return e;
    }
}