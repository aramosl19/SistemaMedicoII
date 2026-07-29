package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
@Getter @Setter
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 0=Compra, 1=Devolución, 2=Venta, 3=Reclamo, 4=Ajuste+, 5=Ajuste-, 6=Despacho automático
    @Column(nullable = false)
    private Integer tipoMovimiento;

    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer stockNuevo;

    @Column(precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(length = 100)
    private String referencia;

    @Column(length = 1000)
    private String motivo;

    @Column(nullable = true)
    private Integer usuarioId; // Para cuando agregues Spring Security

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}