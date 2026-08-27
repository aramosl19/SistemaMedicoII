package org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema;

import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;

import java.util.Optional;

public interface EstadoCitaRepository extends CatalogoRepository<EstadoCita> {
    Optional<EstadoCita> findByNombre(String nombre);
}