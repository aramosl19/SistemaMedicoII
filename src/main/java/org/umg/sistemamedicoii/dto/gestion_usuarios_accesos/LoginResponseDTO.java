package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String nombreUsuario;
    private String rol;
    private String token;
}