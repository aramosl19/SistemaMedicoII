package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.SucursalRequestDTO;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.service.CatalogoService;

import java.util.List;

@RestController
@RequestMapping("/api/sucursales")
public class SucursalController {

    @Autowired
    private CatalogoService<Sucursal> sucursalService;

    @GetMapping
    public List<Sucursal> listar(){
        return sucursalService.listar();
    }

    @GetMapping("/{id}")
    public Sucursal obtenerPorId(@PathVariable Integer id) {
        return sucursalService.obtenerPorId(id);
    }

    @PostMapping
    public Sucursal crear(@Valid @RequestBody SucursalRequestDTO dto) {
        Sucursal sucursal = mapToEntity(new Sucursal(), dto);
        return sucursalService.crear(sucursal);
    }

    @PutMapping("/{id}")
    public Sucursal actualizar(@PathVariable Integer id, @Valid @RequestBody SucursalRequestDTO dto){
        Sucursal sucursal = mapToEntity(new Sucursal(), dto);
        return sucursalService.actualizar(id, sucursal);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        sucursalService.eliminar(id);
    }

    private Sucursal mapToEntity(Sucursal sucursal, SucursalRequestDTO dto) {
        sucursal.setNombre(dto.getNombre());
        sucursal.setDescripcion(dto.getDescripcion());
        sucursal.setTelefono(dto.getTelefono());
        sucursal.setDireccion(dto.getDireccion());
        return sucursal;
    }
}