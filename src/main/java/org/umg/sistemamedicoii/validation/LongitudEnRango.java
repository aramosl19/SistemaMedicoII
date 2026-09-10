package org.umg.sistemamedicoii.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Reemplazo de @Size para los casos donde el documento de casos de uso pide
 * un mensaje con el conteo real de caracteres/dígitos ingresados (ej. "El
 * NIT debe contener entre 8 y 9 caracteres. Usted ingresó 5 caracteres.").
 *
 * Antes esto se intentaba con un mensaje de @Size usando Unified EL
 * (${validatedValue.length()}), pero Hibernate Validator solo evalúa esa
 * sintaxis si hay una implementación de Jakarta EL en el classpath
 * (org.glassfish:jakarta.el / org.glassfish.expressly:expressly); si no
 * está, el mensaje se muestra literal al usuario tal cual, sin interpolar.
 *
 * Esta anotación evita el problema por completo: el validador inyecta el
 * conteo real vía addMessageParameter("longitud", ...), que ParameterMessageInterpolator
 * resuelve directo (igual que {min}/{max}) sin necesitar ninguna
 * implementación de EL.
 *
 * Los campos vacíos/null NO se marcan como error aquí (eso ya lo cubre
 * @NotBlank por separado en el mismo campo), para no duplicar mensajes.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LongitudEnRangoValidator.class)
public @interface LongitudEnRango {

    String message() default "La longitud debe estar entre {min} y {max} caracteres. Usted ingresó {longitud} caracteres.";

    int min();

    int max();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
