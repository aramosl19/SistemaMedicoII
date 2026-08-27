package org.umg.sistemamedicoii.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RecordatorioScheduler {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private EmailService emailService;

    // Se ejecuta todos los días a las 8:00 AM (RN-CU11-05)
    // El formato cron es: "Segundos Minutos Horas Día Mes DíaDeLaSemana"
    @Scheduled(cron = "0 0 8 * * *")
    public void enviarRecordatoriosSeguimiento() {
        // Buscamos citas desde ahora hasta dentro de 48 horas (1 a 2 días)
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = LocalDateTime.now().plusHours(48);

        // Excluimos las canceladas (usamos una consulta que ya tengas o la armamos por código)
        // Para simplificar, traemos todas las de las próximas 48 horas y filtramos en Java
        List<Cita> proximasCitas = citaRepository.findAll().stream()
                .filter(c -> c.getFechaHora().isAfter(desde) && c.getFechaHora().isBefore(hasta))
                .filter(c -> !c.getEstado().getNombre().equalsIgnoreCase("Cancelada"))
                .filter(c -> c.getCitaPadreId() != null) // Solo las de seguimiento
                .toList();

        for (Cita cita : proximasCitas) {
            String asunto = "Recordatorio: Su Cita de Seguimiento se aproxima";
            String mensaje = String.format("Estimado(a) %s,\n\nLe recordamos su cita de seguimiento de tipo: %s.\nFecha: %s\nMédico: %s\nSucursal: %s\n\nEste es un correo automático del Sistema Informático Hospitalario. No responda a este mensaje.",
                    cita.getPaciente().getNombreCompleto(),
                    cita.getTipoSeguimiento(),
                    cita.getFechaHora().toString(),
                    cita.getMedico().getNombreCompleto(),
                    cita.getSucursal().getNombre());

            emailService.enviarCorreo(cita.getPaciente().getCorreo(), asunto, mensaje);
        }
    }
}