package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmergenciaRequestDTO {

    @NotNull(message = "Debe seleccionar la sede.")
    private Integer sucursalId;

    @NotNull(message = "Debe seleccionar la especialidad.")
    private Integer especialidadId;

    @NotNull(message = "Debe seleccionar un médico disponible para atender la emergencia.")
    private Integer medicoId;

    // Opcional: si no se indica, la tarifa/tipo de cita se define después en caja.
    private Integer tipoCitaId;

    private String motivo;
}