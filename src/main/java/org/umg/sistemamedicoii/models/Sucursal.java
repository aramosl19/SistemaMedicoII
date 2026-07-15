package org.umg.sistemamedicoii.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "sucursal")
public class Sucursal extends Catalogo{

    @Column(nullable = true, length = 8)
    private String telefono;

    @Column(nullable = true, length = 500)
    private String direccion;
}
