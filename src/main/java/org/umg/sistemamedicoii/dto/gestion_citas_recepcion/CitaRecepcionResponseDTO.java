package org.umg.sistemamedicoii.dto.gestion_citas_recepcion;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CitaRecepcionResponseDTO {
    private Integer id;
    private Integer pacienteId;
    private String pacienteNombre;
    private String estadoNombre;
    private String especialidadNombre;
    private String sucursalNombre;
    private String medicoNombre;
    private LocalDateTime fechaHora;
    private String motivo;
    private boolean emergencia;
    private LocalDateTime horaLlegada;

    private String mensaje;
}