package org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SucursalRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    // RN-CU15-04: Teléfono de exactamente 8 dígitos si se ingresa
    @Pattern(regexp = "^$|\\d{8}", message = "El teléfono debe tener exactamente 8 dígitos.")
    private String telefono;

    @Size(max = 500, message = "La dirección no puede exceder los 500 caracteres.")
    private String direccion;

    private Boolean activo;
}