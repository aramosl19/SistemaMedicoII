package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
public class BitacoraOperacionResponseDTO {
    private Integer id;
    private String nombreUsuario;
    private String nombreReal;
    private String rol;
    private String operacion;
    private String entidadAfectada;
    private Integer entidadId;
    private LocalDate fecha;
    private LocalTime hora;
}