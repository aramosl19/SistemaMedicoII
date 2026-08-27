package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.CatalogoCie10;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoCie10Repository;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
public class CatalogoCie10Controller {

    @Autowired
    private CatalogoCie10Repository repo;

    @GetMapping
    @Cacheable(value = "catalogos", key = "'cie10_lista'")
    public List<CatalogoCie10> listar() {
        return repo.findAll();
    }
}