package org.umg.sistemamedicoii.models.gestion_usuarios_accesos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria", indexes = @Index(name = "idx_auditoria_fecha_hora", columnList = "fechaHora"))
@Getter @Setter
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String accion;

    @Column(nullable = false, length = 100)
    private String entidadAfectada;

    @Column(nullable = true)
    private Integer entidadId;

    @Column(nullable = false, length = 1000)
    private String detalle;

    @Column(nullable = true)
    private Integer usuarioEjecutorId;

    @Column(nullable = true, length = 150)
    private String nombreUsuario;

    @Column(nullable = true, length = 150)
    private String nombreReal;

    @Column(nullable = true, length = 100)
    private String rol;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}