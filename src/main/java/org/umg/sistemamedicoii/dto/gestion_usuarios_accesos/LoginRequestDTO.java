package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequestDTO {

    @NotBlank(message = "El campo Usuario es obligatorio.")
    private String nombreUsuario;

    @NotBlank(message = "El campo Contraseña es obligatorio.")
    private String password;
}