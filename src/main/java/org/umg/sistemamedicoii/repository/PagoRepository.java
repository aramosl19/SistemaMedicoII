package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {
    boolean existsByCitaId(Integer citaId);
}