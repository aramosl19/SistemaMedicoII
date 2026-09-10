package org.umg.sistemamedicoii.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

public class LongitudEnRangoValidator implements ConstraintValidator<LongitudEnRango, String> {

    private int min;
    private int max;

    @Override
    public void initialize(LongitudEnRango annotation) {
        this.min = annotation.min();
        this.max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank ya reporta por separado el caso vacío/null; aquí no
        // duplicamos ese mensaje.
        if (value == null || value.isBlank()) {
            return true;
        }

        int longitud = value.length();
        if (longitud >= min && longitud <= max) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.unwrap(HibernateConstraintValidatorContext.class)
                .addMessageParameter("longitud", longitud)
                .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addConstraintViolation();
        return false;
    }
}
