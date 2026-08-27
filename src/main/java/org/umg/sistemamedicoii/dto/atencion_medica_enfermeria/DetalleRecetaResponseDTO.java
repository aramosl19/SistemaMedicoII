package org.umg.sistemamedicoii.dto.atencion_medica_enfermeria;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class DetalleRecetaResponseDTO {
    private Integer id;
    private Integer medicamentoId;
    private String medicamentoNombre;
    private String dosis;
    private String frecuencia;
    private String duracion;
    private String indicaciones;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}