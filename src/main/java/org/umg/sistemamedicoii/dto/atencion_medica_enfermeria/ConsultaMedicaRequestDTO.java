package org.umg.sistemamedicoii.dto.atencion_medica_enfermeria;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultaMedicaRequestDTO {

    private String motivoVisita;
    private String hallazgosClinicos;
    private Integer cie10Id;
    private String diagnostico;
    private String planTratamiento;
    private String notasAdicionales;

    // true = el médico quiere cerrar la consulta (estado "Finalizada")
    // false = solo guarda el avance actual (estado "En curso")
    private boolean finalizar;
}