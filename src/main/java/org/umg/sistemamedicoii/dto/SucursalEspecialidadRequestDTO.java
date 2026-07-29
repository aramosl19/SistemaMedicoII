package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SucursalEspecialidadRequestDTO {

    @NotNull(message = "Debe seleccionar una sede.")
    private Integer sucursalId;

    @NotNull(message = "Debe seleccionar una especialidad.")
    private Integer especialidadId;
}