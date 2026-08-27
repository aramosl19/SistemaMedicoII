package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class MovimientoInventarioResponseDTO {
    private Integer id;
    private String tipoMovimientoNombre;
    private String medicamentoNombre;
    private String sucursalNombre;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private BigDecimal costoUnitario;
    private String referencia;
    private String motivo;
    private LocalDateTime fechaHora;
    // Solución CU-15 (gap #1 del QA): "Usuario" es columna obligatoria en la tabla del spec
    private String usuarioNombre;
    // Solución CU-15 (gap #2 del QA): soporta el botón Desactivar/Activar de la tabla
    private boolean activo;
}