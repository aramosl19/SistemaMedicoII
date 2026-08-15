package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.EmergenciaAltaRequestDTO;
import org.umg.sistemamedicoii.dto.EmergenciaRequestDTO;
import org.umg.sistemamedicoii.dto.ReasignarMedicoRequestDTO;

public interface RecepcionService {
    BusquedaRecepcionResponseDTO buscar(Integer numeroCita, String dpi);
    CitaRecepcionResponseDTO registrarLlegada(Integer citaId, boolean emergencia);
    CitaRecepcionResponseDTO reasignarMedico(Integer citaId, ReasignarMedicoRequestDTO dto);
    CitaRecepcionResponseDTO registrarEmergenciaDirecta(Integer pacienteId, EmergenciaRequestDTO dto);

    // FIX CU-05 FA01: alta automática. Si el DPI no existe, crea la cuenta
    // mínima del paciente (nombre + DPI); si existe, usa la cuenta ya
    // registrada. Sede/especialidad/médico se resuelven del Recepcionista
    // autenticado, no del cliente.
    CitaRecepcionResponseDTO registrarEmergenciaConAlta(EmergenciaAltaRequestDTO dto);
}