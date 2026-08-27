package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos;

import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.MovimientoInventarioResponseDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.ResumenMensualInventarioResponseDTO;

import java.util.List;

public interface MovimientoInventarioService {
    List<MovimientoInventarioResponseDTO> listar();
    MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioRequestDTO dto);
    List<ResumenMensualInventarioResponseDTO> generarResumenMensual(Integer sucursalId, int anio, int mes);
    // Solución CU-15 (gap #2 del QA): habilita el botón "Desactivar/Activar" de la tabla
    MovimientoInventarioResponseDTO toggleEstado(Integer id);
}