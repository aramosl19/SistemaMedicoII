package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

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
    // Decisión de cortesía (no exigido por CU): total en Q para que el Kardex
    // mensual pese como reporte financiero, igual que la tabla principal de historial.
    private BigDecimal montoEntradas;
    private BigDecimal montoSalidas;
}