package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.EstadoCitaRequestDTO;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.CatalogoService;

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

    @Auditable(value = "Creó Estado de Cita", entidad = "ESTADO_CITA")
    @PostMapping
    public EstadoCita crear(@Valid @RequestBody EstadoCitaRequestDTO dto) {
        return estadoCitaService.crear(mapToEntity(new EstadoCita(), dto));
    }

    @Auditable(value = "Actualizó Estado de Cita", entidad = "ESTADO_CITA")
    @PutMapping("/{id}")
    public EstadoCita actualizar(@PathVariable Integer id, @Valid @RequestBody EstadoCitaRequestDTO dto) {
        return estadoCitaService.actualizar(id, mapToEntity(new EstadoCita(), dto));
    }

    @Auditable(value = "Eliminó Estado de Cita", entidad = "ESTADO_CITA")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        estadoCitaService.eliminar(id);
    }

    private EstadoCita mapToEntity(EstadoCita e, EstadoCitaRequestDTO dto) {
        e.setNombre(dto.getNombre());
        e.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) e.setActivo(dto.getActivo());
        return e;
    }
}