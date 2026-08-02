package org.umg.sistemamedicoii.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class CitaResponseDTO {
    private Integer id;
    private String pacienteNombre;
    private String medicoNombre;
    private String sucursalNombre;
    private String especialidadNombre;
    private String estadoNombre;
    private LocalDateTime fechaHora;
    private String motivo;
    private Integer citaPadreId;
    private String tipoSeguimiento;
    // Solución CU-12 (gap del QA): RN-CU11-03 exige motivo del seguimiento Y prioridad
    private String prioridadSeguimiento;
}