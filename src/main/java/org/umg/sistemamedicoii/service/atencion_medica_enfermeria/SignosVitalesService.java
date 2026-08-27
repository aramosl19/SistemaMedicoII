package org.umg.sistemamedicoii.service.atencion_medica_enfermeria;

import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.CitaEnfermeriaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.SignosVitalesRequestDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.SignosVitalesResponseDTO;

import java.util.List;

public interface SignosVitalesService {

    List<CitaEnfermeriaResponseDTO> listarCitasPresentes();

    CitaEnfermeriaResponseDTO llamarPaciente(Integer citaId);

    SignosVitalesResponseDTO registrarSignosVitales(Integer citaId, SignosVitalesRequestDTO dto);
}