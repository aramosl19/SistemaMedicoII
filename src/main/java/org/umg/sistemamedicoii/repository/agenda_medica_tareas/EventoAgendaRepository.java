package org.umg.sistemamedicoii.repository.agenda_medica_tareas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.agenda_medica_tareas.EventoAgenda;

import java.time.LocalDateTime;
import java.util.List;

public interface EventoAgendaRepository extends JpaRepository<EventoAgenda, Integer> {
    List<EventoAgenda> findByMedicoIdOrderByFechaInicioAsc(Integer medicoId);

    // GAP QA: un bloqueo de agenda (Bloqueo de disponibilidad, Evento personal,
    // Capacitación o Vacaciones, ver RN-CU14-01) debe ocupar el horario del médico
    // en el calendario. Se buscan los eventos cuyo rango se solapa con [desde, hasta).
    List<EventoAgenda> findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
            Integer medicoId, LocalDateTime hasta, LocalDateTime desde);
}