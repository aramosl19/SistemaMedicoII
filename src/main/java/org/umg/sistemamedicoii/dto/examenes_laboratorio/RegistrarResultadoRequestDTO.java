package org.umg.sistemamedicoii.dto.examenes_laboratorio;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrarResultadoRequestDTO {

    private String valorResultado;
    private String unidad;
    private boolean fueraDeRango;
    private String rangoReferencia;
    private String notasResultado;
}