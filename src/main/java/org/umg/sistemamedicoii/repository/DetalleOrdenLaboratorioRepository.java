package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.DetalleOrdenLaboratorio;

public interface DetalleOrdenLaboratorioRepository extends JpaRepository<DetalleOrdenLaboratorio, Integer> {
}