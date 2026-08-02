package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.models.CatalogoCie10;
import org.umg.sistemamedicoii.repository.CatalogoCie10Repository;

import java.util.List;

@RestController
@RequestMapping("/api/cie10")
public class CatalogoCie10Controller {
    @Autowired private CatalogoCie10Repository repo;

    @GetMapping
    public List<CatalogoCie10> listar() {
        return repo.findAll();
    }
}