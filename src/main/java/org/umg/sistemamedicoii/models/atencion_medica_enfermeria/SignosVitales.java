package org.umg.sistemamedicoii.models.atencion_medica_enfermeria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "signos_vitales")
public class SignosVitales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "enfermero_id", nullable = false)
    private Usuario enfermero;

    @Column(nullable = false)
    private Integer presionSistolica;

    @Column(nullable = false)
    private Integer presionDiastolica;

    // precision = 4 (total dígitos), scale = 1 (decimales) -> ej: 37.5
    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal temperatura;

    // precision = 5, scale = 2 -> ej: 120.50
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal peso;

    // precision = 5, scale = 2 -> ej: 175.00
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal talla;

    @Column(nullable = false)
    private Integer frecuenciaCardiaca;

    // Banderas Clínicas para fácil filtrado y UI
    @Column(nullable = false)
    private boolean alertaPresion = false;

    @Column(nullable = false)
    private boolean alertaTemperatura = false;

    @Column(nullable = false)
    private boolean alertaFrecuencia = false;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro;
}