package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Cita;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CitaRepository extends JpaRepository<Cita, Integer> {
    List<Cita> findByMedicoIdAndFechaHoraBetween(Integer medicoId, LocalDateTime desde, LocalDateTime hasta);

    boolean existsByMedicoIdAndFechaHora(Integer medicoId, LocalDateTime fechaHora);

    List<Cita> findByPaciente_IdAndEstado_NombreNotOrderByFechaHoraAsc(Integer pacienteId, String estadoNombre);

    List<Cita> findByEstado_NombreAndCreadaPorPersonalInternoFalseAndFechaCreacionBefore(
            String estadoNombre, LocalDateTime limite);

    Optional<Cita> findByIdAndEstado_Nombre(Integer id, String estadoNombre);

    List<Cita> findByPaciente_IdAndEstado_NombreOrderByFechaHoraAsc(Integer pacienteId, String estadoNombre);
}