package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerificarDpiResponseDTO {

    private boolean registrado;
    private String rol;
    private String nombreCompleto;
}