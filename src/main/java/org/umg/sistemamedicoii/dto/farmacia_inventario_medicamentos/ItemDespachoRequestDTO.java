package org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ItemDespachoRequestDTO {
    private Integer detalleRecetaId; // ID de lo que recetó el médico
    private Integer cantidadDespachada; // Cantidad final que el paciente decidió comprar
    private Integer medicamentoSustitutoId; // Si hubo sustitución (FA02)
    private String razonSustitucion; // Justificación obligatoria si hay sustituto
}