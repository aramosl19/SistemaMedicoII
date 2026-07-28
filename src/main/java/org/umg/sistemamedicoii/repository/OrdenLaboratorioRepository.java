package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.OrdenLaboratorio;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Integer> {
}