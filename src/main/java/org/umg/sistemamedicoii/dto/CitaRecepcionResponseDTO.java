package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CitaRecepcionResponseDTO {
    private Integer id;
    private String pacienteNombre;
    private String estadoNombre;
    private String especialidadNombre;
    private String sucursalNombre;
    private LocalDateTime fechaHora;
    private String motivo;
    private boolean emergencia;
    private LocalDateTime horaLlegada;

    private String mensaje;
}