package org.umg.sistemamedicoii.models.gestion_usuarios_accesos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria")
@Getter @Setter
public class Auditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String accion; // Ej: "CREAR", "REASIGNAR_MEDICO", "DESPACHO_CONTROLADO"

    @Column(nullable = false, length = 100)
    private String entidadAfectada; // Ej: "USUARIO", "CITA", "MEDICAMENTO"

    @Column(nullable = false)
    private Integer entidadId; // ID del registro afectado

    @Column(nullable = false, length = 1000)
    private String detalle;

    // Aquí está el requerimiento clave de auditor.
    // Es nullable ahora porque no tenemos Spring Security, pero está listo para el futuro.
    @Column(nullable = true)
    private Integer usuarioEjecutorId;

    @Column(nullable = false)
    private LocalDateTime fechaHora;
}