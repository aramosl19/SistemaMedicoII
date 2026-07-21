package org.umg.sistemamedicoii.models;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_cita")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 200))
public class EstadoCita extends Catalogo{
}
