package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class MovimientoInventarioRequestDTO {

    @NotNull(message = "Debe seleccionar un medicamento.")
    private Integer medicamentoId;

    @NotNull(message = "Debe seleccionar una sucursal.")
    private Integer sucursalId;

    @NotNull(message = "Debe seleccionar el tipo de movimiento.")
    private Integer tipoMovimiento;

    @NotNull(message = "La cantidad es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser un número entero positivo.")
    private Integer cantidad;

    private BigDecimal costoUnitario; // Validado en el servicio según el tipo

    private String referencia;

    private String motivo; // Validado en el servicio según el tipo
}