package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsuarioResponseDTO {
    private Integer id;
    private String nombreCompleto;
    private String dpi;
    private String correo;
    private String nombreUsuario;
    private String telefono;
    private String numeroSeguro;
    private String rolNombre;
    private String sucursalNombre;
    private String especialidadNombre;
    private boolean activo;
    // Se hizo esta clase para no poner password
}