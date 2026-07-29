package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.RecetaVigenteResponseDTO;

import java.util.List;

public interface DespachoFarmaciaService {
    List<RecetaVigenteResponseDTO> buscarRecetasVigentes(Integer recetaId, String dpi);
    DespachoFarmaciaResponseDTO despachar(DespachoFarmaciaRequestDTO dto);
    void rechazarReceta(Integer recetaId);
}