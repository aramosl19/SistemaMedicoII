// dto/RecetaRequestDTO.java
package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class RecetaRequestDTO {
    private List<DetalleRecetaRequestDTO> medicamentos;
    private String notas;
}