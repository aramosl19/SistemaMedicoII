package org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.SucursalEspecialidad;

import java.util.List;
import java.util.Optional;

public interface SucursalEspecialidadRepository extends JpaRepository<SucursalEspecialidad, Integer> {

    // Lista todas las asignaciones activas
    List<SucursalEspecialidad> findByActivoTrue();

    // RN-CU12-01: Validación para evitar duplicados de combinaciones activas
    boolean existsBySucursalIdAndEspecialidadIdAndActivoTrue(Integer sucursalId, Integer especialidadId);

    // GAP #4 (RN-CU12-01, índice único): busca la fila sin importar su estado
    // (activa o no), para reutilizarla en vez de insertar una fila nueva cada
    // vez que se reasigna una combinación que antes fue removida. Esto es lo
    // que permite tener un índice único real en (sucursal_id, especialidad_id)
    // sin romper el flujo de "remover y volver a asignar".
    Optional<SucursalEspecialidad> findBySucursalIdAndEspecialidadId(Integer sucursalId, Integer especialidadId);
}