package org.umg.sistemamedicoii.dto.atencion_medica_enfermeria;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CitaPanelMedicoResponseDTO {

    private Integer id;
    private String pacienteNombre;
    private String especialidadNombre;
    private LocalDateTime fechaHora;
    private String estadoNombre;
    private boolean emergencia;
}