package org.umg.sistemamedicoii.repository.agenda_medica_tareas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.agenda_medica_tareas.TareaMedica;

import java.util.List;

public interface TareaMedicaRepository extends JpaRepository<TareaMedica, Integer> {
    List<TareaMedica> findByMedicoIdOrderByCompletadaAscFechaLimiteAsc(Integer medicoId);
}