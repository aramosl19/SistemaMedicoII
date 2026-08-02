package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CitaEnfermeriaResponseDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesRequestDTO;
import org.umg.sistemamedicoii.dto.SignosVitalesResponseDTO;

import java.util.List;

public interface SignosVitalesService {

    List<CitaEnfermeriaResponseDTO> listarCitasPresentes();

    CitaEnfermeriaResponseDTO llamarPaciente(Integer citaId);

    SignosVitalesResponseDTO registrarSignosVitales(Integer citaId, SignosVitalesRequestDTO dto);
}