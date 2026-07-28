package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class RecetaMedicaResponseDTO {
    private Integer id;
    private Integer citaId;
    private String pacienteNombre;
    private String medicoNombre;
    private LocalDateTime fechaEmision;
    private String notas;
    private boolean activo;
    private List<DetalleRecetaResponseDTO> medicamentos;
    private String mensaje;
}