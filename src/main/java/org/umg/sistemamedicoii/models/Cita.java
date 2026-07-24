package org.umg.sistemamedicoii.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @ManyToOne
    @JoinColumn(name = "paciente_id",nullable = false)
    private Usuario paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoCita estado;
    
    @Column (nullable = false)
    private LocalDateTime fechaHora;

    @Column (nullable = false, length = 2000)
    private String motivo;

    @Column (nullable = true)
    private LocalDateTime reservadaHasta;

    @Column(nullable = true)
    private LocalDateTime horaLlegada;

    @Column(nullable = false)
    private boolean emergencia = false;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private boolean creadaPorPersonalInterno = false;

}