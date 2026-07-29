package org.umg.sistemamedicoii.repository;

import org.umg.sistemamedicoii.models.ExamenLaboratorio;

import java.util.List;

public interface ExamenLaboratorioRepository extends CatalogoRepository<ExamenLaboratorio> {
    List<ExamenLaboratorio> findByLaboratorioIdAndActivoTrue(Integer laboratorioId);
}