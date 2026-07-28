package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "detalle_orden_laboratorio")
public class DetalleOrdenLaboratorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenLaboratorio orden;

    @ManyToOne
    @JoinColumn(name = "examen_id", nullable = false)
    private Laboratorio examen;

    // Copia del precio del catálogo al momento de crear la orden,
    // para que un cambio futuro de precio no altere órdenes ya generadas.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = true, length = 200)
    private String valorResultado;

    @Column(nullable = true, length = 50)
    private String unidad;

    @Column(nullable = true)
    private LocalDateTime fechaResultado;

    @Column(nullable = false)
    private boolean fueraDeRango = false;

    @Column(nullable = true, length = 1000)
    private String notasResultado;

    @Column(nullable = false)
    private boolean publicado = false;
}