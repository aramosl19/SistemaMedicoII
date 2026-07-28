package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.ConsultaMedica;

import java.util.Optional;

public interface ConsultaMedicaRepository extends JpaRepository<ConsultaMedica, Integer> {
    Optional<ConsultaMedica> findByCitaId(Integer citaId);
}