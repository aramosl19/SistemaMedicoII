package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PagoRequestDTO {

    @NotNull(message = "Debe indicar la cita a pagar.")
    private Integer citaId;

    @NotBlank(message = "El número de tarjeta es obligatorio.")
    @Pattern(regexp = "\\d{13,19}", message = "El número de tarjeta no es válido.")
    private String numeroTarjeta;

    @NotBlank(message = "El nombre del titular es obligatorio.")
    @Size(min = 5, max = 100, message = "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre del titular debe contener entre 5 y 100 caracteres alfabéticos sin especiales.")
    private String nombreTitular;

    @NotBlank(message = "El vencimiento es obligatorio.")
    @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Formato inválido. Use MM/AA.")
    private String vencimiento;

    @NotBlank(message = "El CVV es obligatorio.")
    @Pattern(regexp = "\\d{3,4}", message = "El CVV no es válido.")
    private String cvv;
}