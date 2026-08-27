package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos;

import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.DespachoFarmaciaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.RecetaMedicaResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.RecetaVigenteResponseDTO;

import java.util.List;

public interface DespachoFarmaciaService {
    // FIX CU-11: se agrega consultaId para permitir búsqueda por ID de Consulta
    List<RecetaVigenteResponseDTO> buscarRecetasVigentes(Integer recetaId, String dpi, Integer consultaId);
    RecetaMedicaResponseDTO obtenerDetalle(Integer recetaId);
    DespachoFarmaciaResponseDTO despachar(DespachoFarmaciaRequestDTO dto);
    // FIX CU-11 FA03: ahora devuelve el mensaje real en vez de void
    String rechazarReceta(Integer recetaId);
}