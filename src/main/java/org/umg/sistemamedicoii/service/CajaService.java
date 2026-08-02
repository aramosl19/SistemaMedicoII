package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.CobroCajaResponseDTO;

import java.util.List;

public interface CajaService {
    List<CitaCobroResponseDTO> buscarCitasPendientes(Integer numeroCita, String dpi);
    // Búsqueda restringida a las citas del propio paciente autenticado.
    List<CitaCobroResponseDTO> buscarCitasPendientesPropias(Integer pacienteId);
    CobroCajaResponseDTO procesarCobro(CobroCajaRequestDTO dto);
}