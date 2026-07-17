package org.umg.sistemamedicoii.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String mensaje) {
        super(mensaje);
    }
}