package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SignosVitalesResponseDTO {

    private Integer id;
    private Integer citaId;
    private String pacienteNombre;

    private Integer presionSistolica;
    private Integer presionDiastolica;
    private BigDecimal temperatura;
    private BigDecimal peso;
    private BigDecimal talla;
    private Integer frecuenciaCardiaca;

    private boolean alertaPresion;
    private boolean alertaTemperatura;
    private boolean alertaFrecuencia;

    private boolean emergencia;
    private LocalDateTime fechaRegistro;

    private String mensaje;
}