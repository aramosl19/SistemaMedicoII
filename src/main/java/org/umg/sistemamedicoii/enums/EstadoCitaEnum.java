package org.umg.sistemamedicoii.enums;

import lombok.Getter;

@Getter
public enum EstadoCitaEnum {
    PENDIENTE_PAGO("Pendiente de pago"),
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada"),
    PACIENTE_PRESENTE("Paciente Presente"),
    SIGNOS_VITALES("Signos Vitales"),
    EN_ESPERA("En Espera"), // <-- Nuevo (Post-signos vitales)
    CONSULTA_MEDICA("Consulta Médica"),
    EVALUADO("Evaluado"), // <-- Nuevo (Post-consulta, pre-cierre)
    ATENCION_FINALIZADA("Atención Finalizada"),
    NO_ASISTIO("No Asistió"); // <-- Nuevo (Flujo Alterno 06)

    private final String nombreBd;

    EstadoCitaEnum(String nombreBd) {
        this.nombreBd = nombreBd;
    }
}