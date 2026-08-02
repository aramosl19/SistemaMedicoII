package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.RegistrarResultadoRequestDTO;

import java.util.List;

public interface LaboratorioResultadoService {

    List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado);

    // FIX QA (gap #2): permite filtrar por el médico solicitante (medicoId == null => sin filtro, todas las órdenes)
    List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado, Integer medicoId);

    OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId);

    // FIX QA (gap #2): valida que el médico solo consulte sus propias órdenes (medicoId == null => sin restricción)
    OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId, Integer medicoId);

    DetalleOrdenLaboratorioResponseDTO registrarResultado(Integer detalleId, RegistrarResultadoRequestDTO dto);

    DetalleOrdenLaboratorioResponseDTO publicarResultado(Integer detalleId);
}