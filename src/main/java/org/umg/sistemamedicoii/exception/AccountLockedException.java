package org.umg.sistemamedicoii.exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String mensaje) {
        super(mensaje);
    }
}