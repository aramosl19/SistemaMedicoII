package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.Laboratorio;

public interface LaboratorioRepository extends JpaRepository<Laboratorio, Integer> {
}
