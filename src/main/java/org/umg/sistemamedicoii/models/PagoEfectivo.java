package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "pago_efectivo")
public class PagoEfectivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoConceptoCobro tipoConcepto;

    @Column(nullable = false)
    private Integer referenciaId;

    @Column(nullable = false, unique = true, length = 36)
    private String numeroTransaccion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cambio;

    @Column(nullable = false)
    private LocalDateTime fechaPago;
}