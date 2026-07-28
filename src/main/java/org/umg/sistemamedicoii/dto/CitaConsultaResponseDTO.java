package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CitaConsultaResponseDTO {

    private Integer id;
    private String pacienteNombre;
    private String estadoNombre;
    private boolean emergencia;

    private String mensaje;
}