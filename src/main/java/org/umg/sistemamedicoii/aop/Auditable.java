package org.umg.sistemamedicoii.aop;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String value();
    String entidad() default "GENERAL";
}