package org.umg.sistemamedicoii.service.atencion_medica_enfermeria;

import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.CitaConsultaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.ConsultaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.ConsultaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.PanelMedicoResponseDTO;

public interface ConsultaMedicaService {

    PanelMedicoResponseDTO obtenerPanel(Integer medicoId);

    CitaConsultaResponseDTO iniciarConsulta(Integer citaId);

    CitaConsultaResponseDTO marcarNoAsistio(Integer citaId);

    ConsultaMedicaResponseDTO guardarConsulta(Integer citaId, ConsultaMedicaRequestDTO dto);

    CitaConsultaResponseDTO finalizarAtencion(Integer citaId);
}