package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos;

import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoResponseDTO;
import java.util.List;

public interface InventarioMedicamentoService {
    List<InventarioMedicamentoResponseDTO> listarInventarioPorSucursal(Integer sucursalId);
    List<InventarioMedicamentoResponseDTO> listarTodo();
}