package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrdenLaboratorioResponseDTO {

    private Integer id;
    private Integer citaId;
    private String pacienteNombre;
    private String medicoNombre;

    private String estado;
    private boolean esExterna;
    private BigDecimal montoTotal;
    private String notas;
    private LocalDateTime fechaCreacion;

    private List<DetalleOrdenLaboratorioResponseDTO> examenes;

    private String mensaje;
}