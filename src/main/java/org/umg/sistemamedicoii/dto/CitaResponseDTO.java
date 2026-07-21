package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CitaResponseDTO {
    private Integer id;
    private String pacienteNombre;
    private String medicoNombre;
    private String sucursalNombre;
    private String especialidadNombre;
    private String estadoNombre;
    private LocalDateTime fechaHora;
    private String motivo;
}
