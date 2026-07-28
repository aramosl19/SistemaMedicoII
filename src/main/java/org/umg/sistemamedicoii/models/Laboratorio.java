package org.umg.sistemamedicoii.models;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "laboratorio")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 500))
public class Laboratorio extends Catalogo {

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
}