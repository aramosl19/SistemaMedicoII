package org.umg.sistemamedicoii.models.agenda_medica_tareas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_agenda")
@Getter @Setter
public class EventoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaFin;

    // 0=Reunión, 1=Descanso, 2=Capacitación, 3=Personal, 4=Otro
    @Column(nullable = false)
    private Integer tipoEvento;

    @Column(nullable = false)
    private boolean todoElDia = false;

    // RN-CU14-01: Por defecto será violeta
    @Column(nullable = false, length = 7)
    private String color = "#8b5cf6";
}