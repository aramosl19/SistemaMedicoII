package org.umg.sistemamedicoii.repository.atencion_medica_enfermeria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.ConsultaMedica;

import java.util.Optional;

public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, Integer> {
    Optional<ConsultaMedica> findByCitaId(Integer citaId);
}