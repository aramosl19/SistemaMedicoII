package org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.InventarioMedicamento;
import java.util.Optional;

public interface InventarioMedicamentoRepository extends JpaRepository<InventarioMedicamento, Integer> {
    Optional<InventarioMedicamento> findByMedicamentoIdAndSucursalId(Integer medicamentoId, Integer sucursalId);
}