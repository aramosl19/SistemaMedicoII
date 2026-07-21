package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Cita;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Integer> {
    List<Cita> findByMedicoIdAndFechaHoraBetween(Integer medicoId, LocalDateTime desde, LocalDateTime hasta);

    boolean existsByMedicoIdAndFechaHora(Integer medicoId, LocalDateTime fechaHora);
}
