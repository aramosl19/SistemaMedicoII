package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.EventoAgenda;

import java.util.List;

public interface EventoAgendaRepository extends JpaRepository<EventoAgenda, Integer> {
    List<EventoAgenda> findByMedicoIdOrderByFechaInicioAsc(Integer medicoId);
}