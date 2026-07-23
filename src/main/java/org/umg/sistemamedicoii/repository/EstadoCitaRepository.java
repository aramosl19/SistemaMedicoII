package org.umg.sistemamedicoii.repository;

import org.umg.sistemamedicoii.models.EstadoCita;

import java.util.Optional;

public interface EstadoCitaRepository extends CatalogoRepository<EstadoCita> {
    Optional<EstadoCita> findByNombre(String nombre);
}