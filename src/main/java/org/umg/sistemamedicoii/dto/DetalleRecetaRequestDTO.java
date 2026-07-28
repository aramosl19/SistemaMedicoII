
package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DetalleRecetaRequestDTO {
    private Integer medicamentoId;
    private String dosis;
    private String frecuencia;
    private String duracion;
    private String indicaciones;
}