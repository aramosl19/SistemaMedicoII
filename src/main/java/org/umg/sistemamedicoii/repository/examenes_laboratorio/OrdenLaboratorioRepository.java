package org.umg.sistemamedicoii.repository.examenes_laboratorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.models.examenes_laboratorio.OrdenLaboratorio;

import java.util.List;

public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, Integer> {
    List<OrdenLaboratorio> findByEstadoOrderByFechaCreacionAsc(EstadoOrdenLaboratorioEnum estado);
    List<OrdenLaboratorio> findByEstadoAndCita_Paciente_DpiOrderByFechaCreacionAsc(EstadoOrdenLaboratorioEnum estado, String dpi);
    // FIX QA (gap #2): permite que un médico consulte solo las órdenes de sus propios pacientes
    List<OrdenLaboratorio> findByEstadoAndMedico_IdOrderByFechaCreacionAsc(EstadoOrdenLaboratorioEnum estado, Integer medicoId);
}