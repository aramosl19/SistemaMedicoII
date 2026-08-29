package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RolRequestDTO {
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres.")
    private String nombre;

    @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres.")
    private String descripcion;

    private Boolean activo;
}