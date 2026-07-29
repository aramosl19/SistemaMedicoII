package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.InventarioMedicamentoResponseDTO;
import java.util.List;

public interface InventarioMedicamentoService {
    List<InventarioMedicamentoResponseDTO> listarInventarioPorSucursal(Integer sucursalId);
    List<InventarioMedicamentoResponseDTO> listarTodo();
}