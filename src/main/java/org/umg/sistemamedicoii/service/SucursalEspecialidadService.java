package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.SucursalEspecialidadRequestDTO;
import org.umg.sistemamedicoii.dto.SucursalEspecialidadResponseDTO;

import java.util.List;

public interface SucursalEspecialidadService {
    List<SucursalEspecialidadResponseDTO> listar();
    SucursalEspecialidadResponseDTO asignar(SucursalEspecialidadRequestDTO dto);
    void eliminar(Integer id);
}