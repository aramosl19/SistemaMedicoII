package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Medicamento;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Integer> {
}
