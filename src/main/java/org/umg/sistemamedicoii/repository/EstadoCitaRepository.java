package org.umg.sistemamedicoii.repository;

import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.EstadoCita;

import java.util.List;
import java.util.Optional;

public interface EstadoCitaRepository extends CatalogoRepository<EstadoCita> {
    
    List<Cita> findByPaciente_IdAndEstado_NombreNotOrderByFechaHoraAsc(Integer pacienteId, String estadoNombre);
    Optional<EstadoCita> findByNombre(String nombre);

}