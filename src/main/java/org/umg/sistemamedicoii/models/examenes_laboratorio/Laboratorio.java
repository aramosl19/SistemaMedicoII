package org.umg.sistemamedicoii.models.examenes_laboratorio;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Catalogo;

@Getter
@Setter
@Entity
@Table(name = "laboratorio")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = false, length = 500))
public class Laboratorio extends Catalogo {
}