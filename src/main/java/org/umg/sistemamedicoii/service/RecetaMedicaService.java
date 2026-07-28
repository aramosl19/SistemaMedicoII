package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaRequestDTO;

public interface RecetaMedicaService {
    RecetaMedicaResponseDTO generarReceta(Integer citaId, RecetaRequestDTO dto);
}