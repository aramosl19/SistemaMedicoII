package org.umg.sistemamedicoii.repository.gestion_usuarios_accesos;

import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Rol;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;

import java.util.Optional;

public interface RolRepository extends CatalogoRepository<Rol> {
    Optional<Rol> findByNombre(String nombre);
}