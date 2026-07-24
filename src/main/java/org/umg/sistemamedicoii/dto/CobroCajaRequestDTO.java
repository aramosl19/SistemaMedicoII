package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class CobroCajaRequestDTO {

    @NotNull(message = "Debe indicar la cita a cobrar.")
    private Integer citaId;

    @NotBlank(message = "Debe seleccionar un método de pago.")
    private String metodoPago;

    private BigDecimal montoRecibido;

    @Pattern(regexp = "\\d{4}", message = "Ingrese los últimos 4 dígitos de la tarjeta.")
    private String ultimosCuatroDigitos;
}