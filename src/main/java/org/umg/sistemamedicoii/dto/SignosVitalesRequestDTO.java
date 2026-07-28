package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SignosVitalesRequestDTO {

    private Integer enfermeroId;

    private Integer presionSistolica;
    private Integer presionDiastolica;
    private BigDecimal temperatura;
    private BigDecimal peso;
    private BigDecimal talla;
    private Integer frecuenciaCardiaca;

    private boolean emergencia;
}