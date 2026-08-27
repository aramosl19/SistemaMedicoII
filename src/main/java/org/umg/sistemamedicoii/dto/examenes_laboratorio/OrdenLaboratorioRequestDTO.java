package org.umg.sistemamedicoii.dto.examenes_laboratorio;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrdenLaboratorioRequestDTO {

    private List<Integer> examenesIds;
    private boolean esExterna;
    private String notas;
}