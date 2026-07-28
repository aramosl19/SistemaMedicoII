package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CitaConsultaResponseDTO;
import org.umg.sistemamedicoii.dto.ConsultaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.ConsultaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.PanelMedicoResponseDTO;

public interface ConsultaMedicaService {

    PanelMedicoResponseDTO obtenerPanel(Integer medicoId);

    CitaConsultaResponseDTO iniciarConsulta(Integer citaId);

    CitaConsultaResponseDTO marcarNoAsistio(Integer citaId);

    ConsultaMedicaResponseDTO guardarConsulta(Integer citaId, ConsultaMedicaRequestDTO dto);

    CitaConsultaResponseDTO finalizarAtencion(Integer citaId);
}