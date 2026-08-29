package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InventarioMedicamentoRequestDTO {

    @NotNull(message = "Debe seleccionar un medicamento.")
    private Integer medicamentoId;

    @NotNull(message = "Debe seleccionar una sede.")
    private Integer sucursalId;

    @NotNull(message = "El stock actual es obligatorio.")
    @Min(value = 0, message = "El stock actual no puede ser negativo.")
    private Integer stockActual;

    private Boolean activo;
}