package org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Catalogo;

import java.util.List;

public interface CatalogoRepository<T extends Catalogo> extends JpaRepository<T, Integer> {
    List<T> findByNombreIgnoreCaseAndActivoTrue(String nombre);

}