package org.umg.sistemamedicoii.repository.atencion_medica_enfermeria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.atencion_medica_enfermeria.RecetaMedica;

import java.util.List;

public interface RecetaMedicaRepository extends JpaRepository<RecetaMedica, Integer> {
    List<RecetaMedica> findByCita_Paciente_DpiAndActivoTrueOrderByFechaEmisionDesc(String dpi);
    // FIX CU-11: búsqueda por ID de Consulta, como pide el flujo normal del spec
    List<RecetaMedica> findByCita_IdAndActivoTrue(Integer citaId);
}