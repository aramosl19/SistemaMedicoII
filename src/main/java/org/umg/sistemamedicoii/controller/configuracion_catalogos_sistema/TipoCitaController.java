package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping
    public TipoCita crear(@RequestBody TipoCita tipoCita) {
        return tipoCitaService.crear(tipoCita);
    }

    @PutMapping("/{id}")
    public TipoCita actualizar(@PathVariable Integer id, @RequestBody TipoCita tipoCita){
        return  tipoCitaService.actualizar(id, tipoCita);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        tipoCitaService.eliminar(id);
    }



}