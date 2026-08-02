package org.umg.sistemamedicoii.exception;

public class AntivirusUnavailableException extends RuntimeException {
    public AntivirusUnavailableException(String mensaje) {
        super(mensaje);
    }
    public AntivirusUnavailableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}