package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.PagoEfectivo;

public interface PagoEfectivoRepository extends JpaRepository<PagoEfectivo, Integer> {
    boolean existsByCitaId(Integer citaId);
}