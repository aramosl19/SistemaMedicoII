package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class PagoResponseDTO {
    private String numeroTransaccion;
    private String medicoNombre;
    private String especialidadNombre;
    private String sucursalNombre;
    private LocalDateTime fechaHoraCita;
    private BigDecimal monto;
    private String mensaje;
}