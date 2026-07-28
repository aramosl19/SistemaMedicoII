package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DetalleOrdenLaboratorioResponseDTO {

    private Integer id;
    private String examenNombre;
    private BigDecimal monto;
    private boolean publicado;
}