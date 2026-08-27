package org.umg.sistemamedicoii.dto.atencion_medica_enfermeria;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ConsultaMedicaResponseDTO {

    private Integer id;
    private Integer citaId;
    private String pacienteNombre;
    private String medicoNombre;

    private String motivoVisita;
    private String hallazgosClinicos;
    private String cie10Codigo;
    private String diagnostico;
    private String planTratamiento;
    private String notasAdicionales;

    private boolean finalizada;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaCierre;

    private String mensaje;
}