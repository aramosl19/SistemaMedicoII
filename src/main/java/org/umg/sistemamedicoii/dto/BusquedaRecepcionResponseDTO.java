package org.umg.sistemamedicoii.dto;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BusquedaRecepcionResponseDTO {

    private  ResultadoBusquedaRecepcionResponseDTO resultado;

    private CitaRecepcionResponseDTO cita;
    private String pacienteNombre;
    private Integer pacienteId;
}
