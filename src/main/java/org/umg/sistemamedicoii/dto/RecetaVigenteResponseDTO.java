package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class RecetaVigenteResponseDTO {
    private Integer id;
    private String pacienteNombre;
    private String medicoNombre;
    private LocalDateTime fechaEmision;
    private int cantidadMedicamentos;
}