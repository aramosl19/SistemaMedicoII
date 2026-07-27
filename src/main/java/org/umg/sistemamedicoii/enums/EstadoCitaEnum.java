package org.umg.sistemamedicoii.enums;

import lombok.Getter;

@Getter
public enum EstadoCitaEnum {
    PENDIENTE_PAGO("Pendiente de pago"),
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada"),
    PACIENTE_PRESENTE("Paciente Presente"),
    SIGNOS_VITALES("Signos Vitales"),
    CONSULTA_MEDICA("Consulta Médica"),
    ATENCION_FINALIZADA("Atención Finalizada");

    private final String nombreBd;

    EstadoCitaEnum(String nombreBd) {
        this.nombreBd = nombreBd;
    }
}