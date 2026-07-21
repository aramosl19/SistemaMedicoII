package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CitaRequestDTO {
    @NotNull(message = "Debe seleccionar un paciente para la cita.")
    private Integer pacienteId;

    @NotNull(message = "Debe seleccionar un medico para continuar.")
    private Integer medicoId;

    @NotNull(message = "Debe seleccionar una sucursal para continuar.")
    private Integer sucursalId;

    @NotNull(message = "Debe seleccionar una especialidad médica para continuar.")
    private Integer especialidadId;

    @NotNull(message = "Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.")
    private LocalDateTime fechaHora;

    @NotBlank(message = "El motivo es obligatorio.")
    @Size(min = 10, max = 2000, message = "El motivo debe contener entre 10 a 2000 caracteres.")
    private String motivo;
}