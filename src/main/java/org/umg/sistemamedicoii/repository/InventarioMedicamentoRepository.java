package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.InventarioMedicamento;
import java.util.Optional;

public interface InventarioMedicamentoRepository extends JpaRepository<InventarioMedicamento, Integer> {
    Optional<InventarioMedicamento> findByMedicamentoIdAndSucursalId(Integer medicamentoId, Integer sucursalId);
}