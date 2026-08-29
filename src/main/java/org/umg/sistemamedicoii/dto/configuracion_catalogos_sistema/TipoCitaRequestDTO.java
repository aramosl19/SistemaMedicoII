package org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class TipoCitaRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    @NotNull(message = "La tarifa base es obligatoria.")
    @DecimalMin(value = "0.01", message = "La tarifa base debe ser mayor a 0.")
    private BigDecimal precio;

    private Boolean activo;
}