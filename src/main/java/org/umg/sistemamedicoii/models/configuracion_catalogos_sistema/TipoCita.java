package org.umg.sistemamedicoii.models.configuracion_catalogos_sistema;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@Entity
@Table(name = "tipo_cita")
@AttributeOverride(name = "descripcion", column = @Column(name = "descripcion", nullable = true, length = 500))
public class TipoCita extends Catalogo {

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;
}