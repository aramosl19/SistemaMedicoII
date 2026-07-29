package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class TareaMedicaResponseDTO {
    private Integer id;
    private String titulo;
    private String descripcion;
    private Integer prioridad;
    private LocalDateTime fechaLimite;
    private boolean completada;
}