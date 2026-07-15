package org.umg.sistemamedicoii.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.service.SucursalServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

    @Autowired
    private SucursalServiceImpl sucursalService;

    @GetMapping
    public List<Sucursal> listar(){
        return sucursalService.listar();
    }

    @GetMapping("/{id}")
    public Sucursal obtenerPorId(@PathVariable Integer id) {
        return sucursalService.obtenerPorId(id);
    }

    @PostMapping
    public Sucursal crear(@RequestBody Sucursal sucursal) {
        return sucursalService.crear(sucursal);
    }

    @PutMapping
    public Sucursal actualizar(@PathVariable Integer id, @RequestBody Sucursal sucursal){
        return sucursalService.actualizar(id, sucursal);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        sucursalService.eliminar(id);
    }
}
