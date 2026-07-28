package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.RegistrarResultadoRequestDTO;

import java.util.List;

public interface LaboratorioResultadoService {

    List<OrdenLaboratorioResponseDTO> listarOrdenes(String estado);

    OrdenLaboratorioResponseDTO obtenerDetalle(Integer ordenId);

    DetalleOrdenLaboratorioResponseDTO registrarResultado(Integer detalleId, RegistrarResultadoRequestDTO dto);

    DetalleOrdenLaboratorioResponseDTO publicarResultado(Integer detalleId);
}