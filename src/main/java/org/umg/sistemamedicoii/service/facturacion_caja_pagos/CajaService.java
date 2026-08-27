package org.umg.sistemamedicoii.service.facturacion_caja_pagos;

import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroCajaResponseDTO;

import java.util.List;

public interface CajaService {
    List<CitaCobroResponseDTO> buscarCitasPendientes(Integer numeroCita, String dpi);
    // Búsqueda restringida a las citas del propio paciente autenticado.
    List<CitaCobroResponseDTO> buscarCitasPendientesPropias(Integer pacienteId);
    CobroCajaResponseDTO procesarCobro(CobroCajaRequestDTO dto);
}