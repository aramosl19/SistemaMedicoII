package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;

public interface RecepcionService {
    BusquedaRecepcionResponseDTO buscar(Integer numeroCita, String dpi);
    CitaRecepcionResponseDTO registrarLlegada(Integer citaId, boolean emergencia);
}
