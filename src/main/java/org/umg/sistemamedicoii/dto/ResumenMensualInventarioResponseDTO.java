package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResumenMensualInventarioResponseDTO {
    private Integer medicamentoId;
    private String medicamentoNombre;
    private Integer totalEntradas;
    private Integer totalSalidas;
    private Integer stockInicial;
    private Integer stockFinal;
    private Integer cantidadMovimientos;
}