package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    @Async
    public void enviarCorreo(String destinatario, String asunto, String mensaje) {
        if (destinatario == null || destinatario.isBlank()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinatario);
        message.setSubject(asunto);
        message.setText(mensaje);
        mailSender.send(message);
    }

    @Override
    public void enviarBienvenida(String destinatario, String nombreCompleto) {
        String asunto = "Bienvenido al Sistema de Citas - Hospital";
        String mensaje = "Estimado(a) " + nombreCompleto + ",\n\n"
                + "Su registro ha sido completado exitosamente. Ya puede agendar sus citas "
                + "médicas a través de nuestro portal.\n\n"
                + "Atentamente,\nSistema de Citas Médicas";
        enviarCorreo(destinatario, asunto, mensaje);
    }
}