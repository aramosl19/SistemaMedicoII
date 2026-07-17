package org.umg.sistemamedicoii.service;

import org.springframework.data.domain.Page;
import org.umg.sistemamedicoii.dto.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.RegistroExternoDTO;
import org.umg.sistemamedicoii.dto.UsuarioRequestDTO;
import org.umg.sistemamedicoii.dto.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.VerificarDpiResponseDTO;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponseDTO> listar();
    UsuarioResponseDTO obtenerPorId(Integer id);
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);
    UsuarioResponseDTO actualizar(Integer id, UsuarioRequestDTO dto);
    void eliminar(Integer id);

    Page<UsuarioResponseDTO> buscar(String campo, String valor, int page, int size);

    UsuarioResponseDTO registrarExterno(RegistroExternoDTO dto);

    VerificarDpiResponseDTO verificarDpi(String dpi);
    LoginResponseDTO login(LoginRequestDTO dto);
}