package org.umg.sistemamedicoii.enums;

public enum EstadoOrdenLaboratorioEnum {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    COMPLETADA("Completada");

    private final String nombre;

    EstadoOrdenLaboratorioEnum(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}