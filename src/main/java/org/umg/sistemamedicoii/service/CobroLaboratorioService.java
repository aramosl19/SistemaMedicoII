package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.CobroLaboratorioResponseDTO;

public interface CobroLaboratorioService {
    CobroLaboratorioResponseDTO cobrar(CobroLaboratorioRequestDTO dto);
}