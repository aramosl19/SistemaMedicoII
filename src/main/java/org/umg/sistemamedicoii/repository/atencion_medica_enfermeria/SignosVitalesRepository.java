package org.umg.sistemamedicoii.repository.atencion_medica_enfermeria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.SignosVitales;

import java.util.Optional;

public interface SignosVitalesRepository extends JpaRepository<SignosVitales, Integer> {
    Optional<SignosVitales> findByCitaId(Integer citaId);
    boolean existsByCitaId(Integer citaId);
}