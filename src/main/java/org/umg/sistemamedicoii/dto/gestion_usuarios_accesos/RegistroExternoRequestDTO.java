package org.umg.sistemamedicoii.dto.gestion_usuarios_accesos;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.validation.LongitudEnRango;

@Getter @Setter
public class RegistroExternoRequestDTO {

    // RN-CU02-01: obligatorio, 10-100 caracteres, mensaje con conteo dinámico.
    // NOTA: se reemplazó el @Size con mensaje en EL (${validatedValue.length()})
    // por @LongitudEnRango, que arma el mismo mensaje con el conteo real sin
    // depender de una implementación de Jakarta EL en el classpath (ver
    // comentario en la anotación). El @Size con EL se mostraba literal, sin
    // interpolar, en el navegador.
    @NotBlank(message = "El nombre debe contener entre 10 y 100 caracteres.")
    @LongitudEnRango(min = 10, max = 100,
            message = "El nombre debe contener entre 10 y 100 caracteres. Usted ingresó {longitud} caracteres.")
    private String nombreCompleto;

    // RN-GLOBAL-001: tres mensajes distintos (obligatorio / longitud / solo
    // numérico). Antes un único @Pattern("\\d{13}") mezclaba longitud y
    // formato numérico en un solo mensaje estático; ahora @Pattern solo
    // valida "solo dígitos" (cualquier longitud) y @LongitudEnRango valida
    // la longitud exacta con el conteo dinámico que pide el documento (sin EL).
    @NotBlank(message = "El campo DPI es obligatorio. Por favor, ingrese su número de DPI.")
    @Pattern(regexp = "\\d*",
            message = "El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.")
    @LongitudEnRango(min = 13, max = 13,
            message = "El DPI debe contener exactamente 13 dígitos. Usted ingresó {longitud} dígitos.")
    private String dpi;

    // RN-GLOBAL-002: obligatorio, 8-9 caracteres (con conteo dinámico), alfanumérico.
    @NotBlank(message = "El campo NIT es obligatorio.")
    @LongitudEnRango(min = 8, max = 9,
            message = "El NIT debe contener entre 8 y 9 caracteres. Usted ingresó {longitud} caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El NIT debe contener únicamente caracteres alfanuméricos.")
    private String nit;

    // RN-CU02-02: obligatorio, exactamente 8 dígitos.
    @NotBlank(message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    @Pattern(regexp = "\\d{8}", message = "El número de teléfono debe contener exactamente 8 dígitos numéricos.")
    private String telefono;

    // RN-CU02-03: opcional; si se ingresa, 5-50 caracteres.
    @Size(min = 5, max = 50, message = "El número de seguro debe contener entre 5 y 50 caracteres.")
    private String numeroSeguro; // opcional

    // RN-CU02-04: obligatorio, formato de correo válido, dominio .com.
    // (Regla que antes solo vivía en el pattern=".*\.com$" del HTML de
    // registro.html; se traslada al backend para no perder la restricción
    // al quitar la capa de validación del navegador.)
    @NotBlank(message = "El campo Correo es obligatorio.")
    @Email(message = "El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com")
    @Pattern(regexp = ".*\\.com$", message = "El correo debe pertenecer a un dominio .com.")
    private String correo;

    // RN-CU02-05: obligatorio, 8-9 caracteres alfanuméricos, con mensajes
    // DISTINTOS para "muy corto" y "muy largo" (el documento los define por
    // separado). @Size es repetible (Jakarta Bean Validation admite varias
    // instancias de la misma anotación en el mismo campo), así que se separa
    // en dos: una solo con min (mensaje "al menos 8") y otra solo con max
    // (mensaje "no puede exceder 9").
    @NotBlank(message = "El usuario debe contener al menos 8 caracteres.")
    @Size(min = 8, message = "El usuario debe contener al menos 8 caracteres.")
    @Size(max = 9, message = "El usuario no puede exceder los 9 caracteres.")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "El usuario debe contener únicamente caracteres alfanuméricos.")
    private String nombreUsuario;

    // RN-CU02-06: obligatorio, mínimo 12 caracteres.
    @NotBlank(message = "La contraseña debe contener al menos 12 caracteres.")
    @Size(min = 12, message = "La contraseña debe contener al menos 12 caracteres.")
    private String password;

    // Confirmación de contraseña: antes solo se comparaba en el navegador
    // (forzarValidezConfirmacion en registro.html) y nunca se enviaba al
    // backend. Se agrega aquí para que la coincidencia se valide también
    // en el servidor una vez retirada esa capa del frontend.
    @NotBlank(message = "Debe confirmar la contraseña.")
    private String confirmarPassword;

    @AssertTrue(message = "Las contraseñas no coinciden.")
    public boolean isPasswordsCoinciden() {
        // Si password o confirmarPassword vienen vacíos, @NotBlank ya los
        // reporta por separado; aquí solo comparamos cuando ambos existen.
        if (password == null || confirmarPassword == null) return true;
        return password.equals(confirmarPassword);
    }
}