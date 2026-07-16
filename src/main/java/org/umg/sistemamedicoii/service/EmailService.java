package org.umg.sistemamedicoii.service;

public interface EmailService {
    void enviarCorreo(String destinatario, String asunto, String mensaje);
    void enviarBienvenida(String destinatario, String nombreCompleto);
}