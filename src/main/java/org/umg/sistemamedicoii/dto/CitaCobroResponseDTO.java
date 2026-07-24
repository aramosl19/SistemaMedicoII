package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class CitaCobroResponseDTO {
    private Integer id;
    private String pacienteNombre;
    private String pacienteDpi;
    private String medicoNombre;
    private String especialidadNombre;
    private String sucursalNombre;
    private LocalDateTime fechaHora;
    private BigDecimal monto;
}