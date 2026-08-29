package org.umg.sistemamedicoii.models.agenda_medica_tareas;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;

import java.time.LocalDateTime;

@Entity
@Table(name = "tarea_medica")
@Getter @Setter
public class TareaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    // 0=Baja, 1=Normal, 2=Alta
    @Column(nullable = false)
    private Integer prioridad = 1;

    @Column(nullable = false)
    private LocalDateTime fechaLimite;

    @Column(nullable = false)
    private boolean completada = false;
}