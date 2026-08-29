package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.TipoCitaRequestDTO;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.TipoCita;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/tipos-cita")
public class TipoCitaController {

    @Autowired
    private CatalogoService<TipoCita> tipoCitaService;

    @GetMapping
    public List<TipoCita> listar() {
        return tipoCitaService.listar();
    }

    @GetMapping("/{id}")
    public TipoCita obtenerPorId(@PathVariable Integer id) {
        return tipoCitaService.obtenerPorId(id);
    }

    @Auditable(value = "Creó Tipo de Cita", entidad = "TIPO_CITA")
    @PostMapping
    public TipoCita crear(@Valid @RequestBody TipoCitaRequestDTO dto) {
        return tipoCitaService.crear(mapToEntity(new TipoCita(), dto));
    }

    @Auditable(value = "Actualizó Tipo de Cita", entidad = "TIPO_CITA")
    @PutMapping("/{id}")
    public TipoCita actualizar(@PathVariable Integer id, @Valid @RequestBody TipoCitaRequestDTO dto){
        return tipoCitaService.actualizar(id, mapToEntity(new TipoCita(), dto));
    }

    @Auditable(value = "Eliminó Tipo de Cita", entidad = "TIPO_CITA")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        tipoCitaService.eliminar(id);
    }

    private TipoCita mapToEntity(TipoCita t, TipoCitaRequestDTO dto) {
        t.setNombre(dto.getNombre());
        t.setDescripcion(dto.getDescripcion());
        t.setPrecio(dto.getPrecio());
        if (dto.getActivo() != null) t.setActivo(dto.getActivo());
        return t;
    }
}