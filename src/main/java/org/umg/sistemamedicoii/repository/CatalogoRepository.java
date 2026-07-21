package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Catalogo;

import java.util.List;

public interface CatalogoRepository<T extends Catalogo> extends JpaRepository<T, Integer> {
    List<T> findByNombreIgnoreCaseAndActivoTrue(String nombre);
}