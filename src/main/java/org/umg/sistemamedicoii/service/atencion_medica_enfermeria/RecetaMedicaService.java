package org.umg.sistemamedicoii.service.atencion_medica_enfermeria;

import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaRequestDTO;

public interface RecetaMedicaService {
    RecetaMedicaResponseDTO generarReceta(Integer citaId, RecetaRequestDTO dto);
}