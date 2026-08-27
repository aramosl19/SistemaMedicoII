package org.umg.sistemamedicoii.service.gestion_usuarios_acceso;

import org.springframework.data.domain.Page;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiResponseDTO;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponseDTO> listar();
    UsuarioResponseDTO obtenerPorId(Integer id);
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);
    UsuarioResponseDTO actualizar(Integer id, UsuarioRequestDTO dto);
    void eliminar(Integer id);

    Page<UsuarioResponseDTO> buscar(String campo, String valor, int page, int size);

    UsuarioResponseDTO registrarExterno(RegistroExternoRequestDTO dto);

    VerificarDpiResponseDTO verificarDpi(String dpi);
    LoginResponseDTO login(LoginRequestDTO dto);
}