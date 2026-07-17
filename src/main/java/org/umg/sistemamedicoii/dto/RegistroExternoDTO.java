package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistroExternoDTO {

    @NotBlank(message = "El nombre debe contener entre 10 y 100 caracteres.")
    @Size(min = 10, max = 100, message = "El nombre debe contener entre 10 y 100 caracteres.")
    private String nombreCompleto;

    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "\\d{13}", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    @NotBlank(message = "El campo NIT es obligatorio.")
    @Size(min = 8, max = 9, message = "El NIT debe contener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El NIT debe contener únicamente caracteres alfanuméricos.")
    private String nit;

    @NotBlank(message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    @Pattern(regexp = "\\d{8}", message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    private String telefono;

    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro; // opcional

    @NotBlank(message = "El campo Correo es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com")
    private String correo;

    @NotBlank(message = "El usuario debe contener al menos 8 caracteres.")
    @Size(min = 8, max = 9, message = "El usuario debe tener entre 8 y 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    @NotBlank(message = "La contraseña debe contener al menos 12 caracteres.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;
}