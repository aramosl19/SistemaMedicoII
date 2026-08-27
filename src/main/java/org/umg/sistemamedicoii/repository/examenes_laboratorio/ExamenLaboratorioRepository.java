package org.umg.sistemamedicoii.repository.examenes_laboratorio;

import org.umg.sistemamedicoii.models.examenes_laboratorio.ExamenLaboratorio;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;

import java.util.List;

public interface ExamenLaboratorioRepository extends CatalogoRepository<ExamenLaboratorio> {
    List<ExamenLaboratorio> findByLaboratorioIdAndActivoTrue(Integer laboratorioId);
}