package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.DetalleRecetaResponseDTO;

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