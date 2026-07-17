package org.umg.sistemamedicoii.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.umg.sistemamedicoii.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByDpi(String dpi);

    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);

    boolean existsByNombreUsuarioAndIdNot(String nombreUsuario, Integer id);

    boolean existsByDpiAndIdNot(String dpi, Integer id);


    @Query(value = "SELECT * FROM usuario u WHERE unaccent(u.nombre_completo) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            countQuery = "SELECT count(*) FROM usuario u WHERE unaccent(u.nombre_completo) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            nativeQuery = true)
    Page<Usuario> buscarPorNombre(@Param("valor") String valor, Pageable pageable);

    @Query(value = "SELECT u.* FROM usuario u JOIN rol r ON u.rol_id = r.id WHERE unaccent(r.nombre) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            countQuery = "SELECT count(*) FROM usuario u JOIN rol r ON u.rol_id = r.id WHERE unaccent(r.nombre) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            nativeQuery = true)
    Page<Usuario> buscarPorRol(@Param("valor") String valor, Pageable pageable);

    @Query(value = "SELECT * FROM usuario u WHERE unaccent(u.correo) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            countQuery = "SELECT count(*) FROM usuario u WHERE unaccent(u.correo) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            nativeQuery = true)
    Page<Usuario> buscarPorCorreo(@Param("valor") String valor, Pageable pageable);

    @Query(value = "SELECT * FROM usuario u WHERE unaccent(u.nombre_usuario) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            countQuery = "SELECT count(*) FROM usuario u WHERE unaccent(u.nombre_usuario) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            nativeQuery = true)
    Page<Usuario> buscarPorNombreUsuario(@Param("valor") String valor, Pageable pageable);

    @Query(value = "SELECT * FROM usuario u WHERE unaccent(u.dpi) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            countQuery = "SELECT count(*) FROM usuario u WHERE unaccent(u.dpi) ILIKE unaccent(CONCAT('%', :valor, '%'))",
            nativeQuery = true)
    Page<Usuario> buscarPorDpi(@Param("valor") String valor, Pageable pageable);
}