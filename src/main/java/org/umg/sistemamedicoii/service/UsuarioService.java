package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.UsuarioRequestDTO;
import org.umg.sistemamedicoii.dto.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {
    List<UsuarioResponseDTO> listar();
    UsuarioResponseDTO obtenerPorId(Integer id);
    UsuarioResponseDTO crear(UsuarioRequestDTO dto);
    UsuarioResponseDTO actualizar(Integer id, UsuarioRequestDTO dto);
    void eliminar(Integer id);
}