package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InventarioMedicamentoResponseDTO {
    private Integer id;
    private String medicamentoNombre;
    private String sucursalNombre;
    private Integer stockActual;
    private Integer stockMinimo;
    private boolean alertaStockBajo;
    private boolean medicamentoControlado;
}