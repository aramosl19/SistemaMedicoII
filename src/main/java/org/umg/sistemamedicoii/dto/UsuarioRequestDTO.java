package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsuarioRequestDTO {

    @NotBlank(message = "El campo Nombre es obligatorio.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String nombreCompleto;

    @Pattern(regexp = "\\d{13}", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    private String nit;

    @NotBlank(message = "El campo Correo es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido.")
    private String correo;

    @NotBlank(message = "El campo Usuario es obligatorio.")
    @Size(min = 8, max = 9, message = "El usuario debe tener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;

    @Pattern(regexp = "\\d{8}", message = "El teléfono debe contener exactamente 8 dígitos.")
    private String telefono; // opcional

    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro; // opcional

    @NotNull(message = "Debe seleccionar un rol para el usuario.")
    private Integer rolId;

    private Integer sucursalId;      // sin anotación: obligatorio solo en creación
    private Integer especialidadId;  // sin anotación: obligatorio solo si rol = Médico

    private Boolean activo;
}