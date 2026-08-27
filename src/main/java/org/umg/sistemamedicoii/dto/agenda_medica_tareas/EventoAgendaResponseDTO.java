package org.umg.sistemamedicoii.dto.agenda_medica_tareas;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class EventoAgendaResponseDTO {
    private Integer id;
    private Integer medicoId;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer tipoEvento;
    private boolean todoElDia;
    private String color;
}