package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerificarDpiRequestDTO {

    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "\\d{13}", message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;
}