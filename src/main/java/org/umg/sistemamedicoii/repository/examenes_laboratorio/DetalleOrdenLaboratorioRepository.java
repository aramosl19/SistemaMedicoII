package org.umg.sistemamedicoii.repository.examenes_laboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.examenes_laboratorio.DetalleOrdenLaboratorio;

public interface DetalleOrdenLaboratorioRepository extends JpaRepository<DetalleOrdenLaboratorio, Integer> {
}