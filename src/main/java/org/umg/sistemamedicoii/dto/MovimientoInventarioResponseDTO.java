package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class MovimientoInventarioResponseDTO {
    private Integer id;
    private String tipoMovimientoNombre;
    private String medicamentoNombre;
    private String sucursalNombre;
    private Integer cantidad;
    private Integer stockAnterior;
    private Integer stockNuevo;
    private BigDecimal costoUnitario;
    private String referencia;
    private String motivo;
    private LocalDateTime fechaHora;
}