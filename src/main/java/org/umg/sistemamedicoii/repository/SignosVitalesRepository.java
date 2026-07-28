package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.SignosVitales;

import java.util.Optional;

public interface SignosVitalesRepository extends JpaRepository<SignosVitales, Integer> {
    Optional<SignosVitales> findByCitaId(Integer citaId);
    boolean existsByCitaId(Integer citaId);
}