package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InventarioMedicamentoResponseDTO {
    private Integer id;
    private Integer medicamentoId;
    private String medicamentoNombre;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer stockActual;
    private Integer stockMinimo;
    private boolean alertaStockBajo;
    private boolean medicamentoControlado;
    private boolean activo;
}