package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "examen_laboratorio")
public class ExamenLaboratorio extends Catalogo {

    @ManyToOne
    @JoinColumn(name = "laboratorio_id", nullable = false)
    private Laboratorio laboratorio;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "rango_referencia", nullable = true, length = 100)
    private String rangoReferencia;

    @Column(name = "unidad_medida", nullable = true, length = 50)
    private String unidadMedida;
}