package org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {
    List<MovimientoInventario> findAllByOrderByFechaHoraDesc();

    // Trae los movimientos para el resumen mensual en el orden cronológico adecuado
    List<MovimientoInventario> findBySucursalIdAndFechaHoraBetweenOrderByFechaHoraAsc(Integer sucursalId, LocalDateTime desde, LocalDateTime hasta);
}