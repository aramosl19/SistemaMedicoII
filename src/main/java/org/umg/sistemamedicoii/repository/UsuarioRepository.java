package org.umg.sistemamedicoii.repository;

import org.umg.sistemamedicoii.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    Optional<Usuario> findByDpi(String dpi);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);

    boolean existsByNombreUsuarioAndIdNot(String nombreUsuario, Integer id);

    boolean existsByDpiAndIdNot(String dpi, Integer id);
}