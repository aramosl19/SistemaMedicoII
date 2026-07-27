package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.Especialidad;
import org.umg.sistemamedicoii.models.TipoCita;
import org.umg.sistemamedicoii.service.CatalogoService;

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