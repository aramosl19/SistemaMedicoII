package org.umg.sistemamedicoii.models;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "especialidad")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 500))
public class Especialidad extends Catalogo {
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal precio;
}