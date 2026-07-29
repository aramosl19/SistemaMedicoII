package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.TareaMedica;

import java.util.List;

public interface TareaMedicaRepository extends JpaRepository<TareaMedica, Integer> {
    List<TareaMedica> findByMedicoIdOrderByCompletadaAscFechaLimiteAsc(Integer medicoId);
}