package org.umg.sistemamedicoii.dto.gestion_citas_recepcion;

public class ReasignarMedicoRequestDTO {

    private Integer nuevoMedicoId;
    private String motivoReasignacion;

    public Integer getNuevoMedicoId() {
        return nuevoMedicoId;
    }

    public void setNuevoMedicoId(Integer nuevoMedicoId) {
        this.nuevoMedicoId = nuevoMedicoId;
    }

    public String getMotivoReasignacion() {
        return motivoReasignacion;
    }

    public void setMotivoReasignacion(String motivoReasignacion) {
        this.motivoReasignacion = motivoReasignacion;
    }
}