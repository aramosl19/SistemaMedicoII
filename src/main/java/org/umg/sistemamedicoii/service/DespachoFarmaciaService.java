package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaVigenteResponseDTO;

import java.util.List;

public interface DespachoFarmaciaService {
    // FIX CU-11: se agrega consultaId para permitir búsqueda por ID de Consulta
    List<RecetaVigenteResponseDTO> buscarRecetasVigentes(Integer recetaId, String dpi, Integer consultaId);
    RecetaMedicaResponseDTO obtenerDetalle(Integer recetaId);
    DespachoFarmaciaResponseDTO despachar(DespachoFarmaciaRequestDTO dto);
    // FIX CU-11 FA03: ahora devuelve el mensaje real en vez de void
    String rechazarReceta(Integer recetaId);
}