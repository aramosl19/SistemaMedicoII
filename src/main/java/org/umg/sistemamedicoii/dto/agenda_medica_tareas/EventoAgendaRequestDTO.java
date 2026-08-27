package org.umg.sistemamedicoii.dto.agenda_medica_tareas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class EventoAgendaRequestDTO {

    @NotBlank(message = "El título del evento es obligatorio.")
    @Size(min = 5, max = 200, message = "El título debe contener entre 5 y 200 caracteres.")
    private String titulo;

    @Size(max = 2000, message = "La descripción no puede exceder los 2000 caracteres.")
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria.")
    private LocalDateTime fechaFin;

    @NotNull(message = "Debe seleccionar un tipo de evento.")
    private Integer tipoEvento;

    private boolean todoElDia;
}