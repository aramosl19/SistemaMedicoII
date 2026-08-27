package org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SucursalEspecialidadResponseDTO {
    private Integer id;
    private Integer sucursalId;
    private String sucursalNombre;
    private Integer especialidadId;
    private String especialidadNombre;
    private boolean activo;
}