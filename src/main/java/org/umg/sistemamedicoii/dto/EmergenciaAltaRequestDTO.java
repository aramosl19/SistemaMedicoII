package org.umg.sistemamedicoii.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// FIX CU-05 FA01: DTO para el ingreso de emergencia CON alta automática de
// paciente. A diferencia de EmergenciaRequestDTO (que exige un pacienteId ya
// existente + sucursalId/especialidadId/medicoId elegidos a mano), este
// endpoint solo pide lo que el documento exige en el paso 2 de FA01: nombre y
// DPI del paciente. Todo lo demás (sede, especialidad, médico) se resuelve en
// el backend a partir del Recepcionista autenticado — ver
// RecepcionServiceImpl.registrarEmergenciaConAlta().
@Getter @Setter
public class EmergenciaAltaRequestDTO {

    // Mismas reglas que RegistroExternoRequestDTO.nombreCompleto (RN-CU02-01),
    // para que un paciente que después complete su registro no encuentre un
    // nombre que ya no pasaría esa misma validación.
    @NotBlank(message = "El nombre debe contener entre 10 y 100 caracteres.")
    @Size(min = 10, max = 100,
            message = "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó ${validatedValue.length()} caracteres.")
    private String nombrePaciente;

    // A diferencia de nombre/NIT (que son RANGOS de longitud, donde saber
    // "cuánto te faltó" ayuda), el DPI es una longitud FIJA de 13 — el
    // conteo dinámico no aporta nada útil aquí. Mensaje estático, igual
    // estilo que ya usa "telefono" (otro campo de longitud fija) en el
    // resto del sistema.
    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "\\d*",
            message = "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.")
    @Size(min = 13, max = 13, message = "El DPI debe contener exactamente 13 dígitos.")
    private String dpi;

    // Opcional, igual que en EmergenciaRequestDTO.
    private String motivo;
}