package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class DespachoFarmaciaRequestDTO {

    @NotNull(message = "Debe indicar la receta a despachar.")
    private Integer recetaId;

    @NotNull(message = "Debe enviar los items a despachar.")
    private List<ItemDespachoRequestDTO> items;

}