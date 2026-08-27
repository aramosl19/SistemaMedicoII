package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema;

import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadRequestDTO;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadResponseDTO;

import java.util.List;

public interface SucursalEspecialidadService {
    List<SucursalEspecialidadResponseDTO> listar();
    SucursalEspecialidadResponseDTO asignar(SucursalEspecialidadRequestDTO dto);
    void eliminar(Integer id);
}