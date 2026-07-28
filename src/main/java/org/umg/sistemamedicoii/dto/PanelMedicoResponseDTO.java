package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PanelMedicoResponseDTO {

    private List<CitaPanelMedicoResponseDTO> enEsperaDeConsulta;
    private List<CitaPanelMedicoResponseDTO> enConsultaMedica;
    private List<CitaPanelMedicoResponseDTO> evaluadosPendienteCierre;
}