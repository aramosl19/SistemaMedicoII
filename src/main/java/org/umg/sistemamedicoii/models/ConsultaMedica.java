package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "consulta_medica")
public class ConsultaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "cie10_id", nullable = true)
    private CatalogoCie10 cie10;

    @Column(nullable = true, length = 2000)
    private String motivoVisita;

    @Column(nullable = true, length = 4000)
    private String hallazgosClinicos;

    @Column(nullable = true, length = 5000)
    private String diagnostico;

    @Column(nullable = true, length = 4000)
    private String planTratamiento;

    @Column(nullable = true, length = 2000)
    private String notasAdicionales;

    @Column(nullable = false)
    private boolean finalizada = false;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = true)
    private LocalDateTime fechaCierre;
}