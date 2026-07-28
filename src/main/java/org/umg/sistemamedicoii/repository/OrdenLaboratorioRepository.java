package org.umg.sistemamedicoii.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.models.OrdenLaboratorio;

import java.util.List;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Integer> {
    List<OrdenLaboratorio> findByEstadoOrderByFechaCreacionAsc(EstadoOrdenLaboratorioEnum estado);
}