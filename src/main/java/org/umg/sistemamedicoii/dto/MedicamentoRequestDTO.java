package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class MedicamentoRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    // RN-CU15-02: Precio mayor a 0
    @NotNull(message = "El precio es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0.")
    private BigDecimal precio;

    @NotBlank(message = "La unidad es obligatoria.")
    @Size(max = 50, message = "La unidad no puede exceder los 50 caracteres.")
    private String unidad;

    private boolean controlled;

    private Integer minimumStock;
}