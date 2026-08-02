package org.umg.sistemamedicoii.exception;

public class VirusDetectedException extends RuntimeException {
    public VirusDetectedException(String mensaje) {
        super(mensaje);
    }
}