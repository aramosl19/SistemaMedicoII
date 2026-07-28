package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;

public interface OrdenLaboratorioService {

    OrdenLaboratorioResponseDTO generarOrden(Integer citaId, OrdenLaboratorioRequestDTO dto);
}