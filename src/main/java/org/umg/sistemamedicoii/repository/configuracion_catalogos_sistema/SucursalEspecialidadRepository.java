package org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.SucursalEspecialidad;

import java.util.List;

public interface SucursalEspecialidadRepository extends JpaRepository<SucursalEspecialidad, Integer> {

    // Lista todas las asignaciones activas
    List<SucursalEspecialidad> findByActivoTrue();

    // RN-CU12-01: Validación para evitar duplicados de combinaciones activas
    boolean existsBySucursalIdAndEspecialidadIdAndActivoTrue(Integer sucursalId, Integer especialidadId);
}