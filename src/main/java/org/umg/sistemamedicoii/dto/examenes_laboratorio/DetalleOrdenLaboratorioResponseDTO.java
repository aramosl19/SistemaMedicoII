package org.umg.sistemamedicoii.dto.examenes_laboratorio;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DetalleOrdenLaboratorioResponseDTO {

    private Integer id;
    private String examenNombre;
    private BigDecimal monto;

    private String valorResultado;
    private String unidad;
    private LocalDateTime fechaResultado;
    private boolean fueraDeRango;
    private String rangoReferencia;
    private String notasResultado;

    private boolean publicado;
    private String mensaje;
}