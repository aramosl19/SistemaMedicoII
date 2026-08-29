package org.umg.sistemamedicoii.models.configuracion_catalogos_sistema;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "sucursal_especialidad",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sucursal_especialidad",
                columnNames = {"sucursal_id", "especialidad_id"}
        )
)
@Getter @Setter
public class SucursalEspecialidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false)
    private boolean activo = true;
}