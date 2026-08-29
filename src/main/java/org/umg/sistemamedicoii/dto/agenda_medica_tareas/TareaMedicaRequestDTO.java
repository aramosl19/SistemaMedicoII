package org.umg.sistemamedicoii.dto.agenda_medica_tareas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class TareaMedicaRequestDTO {

    @NotBlank(message = "El título de la tarea es obligatorio.")
    @Size(min = 5, max = 200, message = "El título debe contener entre 5 y 200 caracteres.")
    private String titulo;

    @Size(max = 1000, message = "La descripción no puede exceder los 1000 caracteres.")
    private String descripcion;

    @NotNull(message = "La prioridad de la tarea es obligatoria.")
    private Integer prioridad;

    @NotNull(message = "La fecha límite de la tarea es obligatoria.")
    private LocalDateTime fechaLimite;

    private boolean completada;
}