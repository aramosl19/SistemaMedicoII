package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class DespachoFarmaciaResponseDTO {
    private String numeroTransaccion;
    private Integer recetaId;
    private String pacienteNombre;
    private String sucursalNombre;
    private List<DetalleRecetaResponseDTO> medicamentosDespachados;
    private List<String> alertasStock;
    private String mensaje;
}