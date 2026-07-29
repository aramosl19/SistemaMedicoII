package org.umg.sistemamedicoii.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "inventario_medicamento", uniqueConstraints = {@UniqueConstraint(columnNames = {"medicamento_id", "sucursal_id"})})
@Getter @Setter
public class InventarioMedicamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @ManyToOne @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(nullable = false)
    private Integer stockActual = 0;

    @Column(nullable = false)
    private boolean activo = true;
}