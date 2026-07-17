package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerificarDpiResponseDTO {

    private boolean registrado;
    private String rol;
    private String nombreCompleto;
}