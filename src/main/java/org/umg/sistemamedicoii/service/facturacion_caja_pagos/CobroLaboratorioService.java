package org.umg.sistemamedicoii.service.facturacion_caja_pagos;

import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;

import java.util.List;

public interface CobroLaboratorioService {
    CobroLaboratorioResponseDTO cobrar(CobroLaboratorioRequestDTO dto);

    List<OrdenLaboratorioResponseDTO> buscarOrdenesPendientes(Integer numeroOrden, String dpi);
}