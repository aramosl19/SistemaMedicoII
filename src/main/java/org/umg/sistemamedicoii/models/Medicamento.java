package org.umg.sistemamedicoii.models;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "medicamento")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 500))
public class Medicamento extends Catalogo{
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false, length = 50)
    private String unidad;

    @Column(nullable = false)
    private boolean controlled;

    @Column(nullable = true)
    private Integer minimumStock;

}
