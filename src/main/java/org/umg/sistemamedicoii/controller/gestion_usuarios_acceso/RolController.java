package org.umg.sistemamedicoii.controller.gestion_usuarios_acceso;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RolRequestDTO;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Rol;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl.RolServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolServiceImpl rolService;

    @GetMapping
    public List<Rol> listar() {
        return rolService.listar();
    }

    @GetMapping("/{id}")
    public Rol obtenerPorId(@PathVariable Integer id) {
        return rolService.obtenerPorId(id);
    }

    @Auditable(value = "Creó Rol", entidad = "ROL")
    @PostMapping
    public Rol crear(@Valid @RequestBody RolRequestDTO dto) {
        return rolService.crear(mapToEntity(new Rol(), dto));
    }

    @Auditable(value = "Actualizó Rol", entidad = "ROL")
    @PutMapping("/{id}")
    public Rol actualizar(@PathVariable Integer id, @Valid @RequestBody RolRequestDTO dto) {
        return rolService.actualizar(id, mapToEntity(new Rol(), dto));
    }

    @Auditable(value = "Eliminó Rol", entidad = "ROL")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        rolService.eliminar(id);
    }

    private Rol mapToEntity(Rol r, RolRequestDTO dto) {
        r.setNombre(dto.getNombre());
        r.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) r.setActivo(dto.getActivo());
        return r;
    }
}