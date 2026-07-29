package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.MovimientoInventarioResponseDTO;

import java.util.List;

public interface MovimientoInventarioService {
    List<MovimientoInventarioResponseDTO> listar();
    MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioRequestDTO dto);
}