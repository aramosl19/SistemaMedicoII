package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;

import java.util.List;

public interface CobroLaboratorioService {
    CobroLaboratorioResponseDTO cobrar(CobroLaboratorioRequestDTO dto);

    List<OrdenLaboratorioResponseDTO> buscarOrdenesPendientes(Integer numeroOrden, String dpi);
}