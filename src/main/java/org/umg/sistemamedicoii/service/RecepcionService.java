package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.ReasignarMedicoRequestDTO;

public interface RecepcionService {
    BusquedaRecepcionResponseDTO buscar(Integer numeroCita, String dpi);
    CitaRecepcionResponseDTO registrarLlegada(Integer citaId, boolean emergencia);
    CitaRecepcionResponseDTO reasignarMedico(Integer citaId, ReasignarMedicoRequestDTO dto);
}