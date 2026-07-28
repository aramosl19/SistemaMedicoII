package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "receta_medica")
public class RecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @Column(nullable = true, length = 1000)
    private String notas;

    // RN-CU10-01: vigencia de 7 días desde fechaEmision.
    // "activo" es el state=1 que CU-11 usa para filtrar solo recetas vigentes
    // en el buscador de farmacia (no se muestran recetas ya despachadas/vencidas).

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleReceta> detalles = new ArrayList<>();
}