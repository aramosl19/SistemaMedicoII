package org.umg.sistemamedicoii.dto.examenes_laboratorio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExamenLaboratorioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
    private String nombre;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    @NotNull(message = "El precio base es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio base debe ser mayor a 0.")
    private BigDecimal precio;

    @NotNull(message = "Debe seleccionar un laboratorio.")
    private Integer laboratorioId;

    @Size(max = 100, message = "El rango de referencia no puede exceder los 100 caracteres.")
    private String rangoReferencia;

    @Size(max = 50, message = "La unidad de medida no puede exceder los 50 caracteres.")
    private String unidadMedida;

    private Boolean activo;
}