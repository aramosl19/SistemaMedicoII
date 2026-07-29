package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_key", uniqueConstraints = @UniqueConstraint(columnNames = "clave"))
@Getter @Setter
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(nullable = false)
    private Integer citaId;

    @Column(nullable = false)
    private String numeroTransaccion;

    @Column(nullable = false)
    private String medicoNombre;

    @Column(nullable = false)
    private String especialidadNombre;

    @Column(nullable = false)
    private String sucursalNombre;

    @Column(nullable = false)
    private LocalDateTime fechaHoraCita;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}