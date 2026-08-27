package org.umg.sistemamedicoii.models.gestion_usuarios_accesos;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Catalogo;

@Entity
@Table(name = "rol")
public class Rol extends Catalogo {
}