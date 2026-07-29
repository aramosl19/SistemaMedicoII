package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.MovimientoInventario;

import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {
    List<MovimientoInventario> findAllByOrderByFechaHoraDesc();
}