package org.umg.sistemamedicoii.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombreCompleto;

    @Column(nullable = true,unique = true, length = 13)
    private String dpi;

    @Column(nullable = true)
    private String nit;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false, unique = true)
    private String nombreUsuario;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String telefono;

    @Column(nullable = true)
    private String numeroSeguro;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = true)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = true)
    private Especialidad especialidad;

    @Column(nullable = false)
    private boolean activo = true;

    // --- Control de login fallido (RN-CU00-03) ---
    @Column(nullable = false)
    private int intentosFallidos = 0;

    @Column(nullable = true)
    private LocalDateTime bloqueadoHasta;
}