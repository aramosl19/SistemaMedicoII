package org.umg.sistemamedicoii.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.EstadoCita;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.EstadoCitaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CitaCancelacionScheduler {

    private static final String ESTADO_PENDIENTE_PAGO = "Pendiente de pago";
    private static final String ESTADO_CANCELADA = "Cancelada";
    private static final int MINUTOS_LIMITE = 10;

    @Autowired private CitaRepository citaRepository;
    @Autowired private EstadoCitaRepository estadoCitaRepository;

    // FA03 (CU-06): citas agendadas desde el portal con más de 10 minutos
    // en "Pendiente de pago" se cancelan automáticamente. Las citas creadas
    // por personal interno (walk-in) no se cancelan.
    @Scheduled(fixedRate = 60000)
    public void cancelarCitasPendientesVencidas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_LIMITE);

        List<Cita> vencidas = citaRepository
                .findByEstado_NombreAndCreadaPorPersonalInternoFalseAndFechaCreacionBefore(
                        ESTADO_PENDIENTE_PAGO, limite);

        if (vencidas.isEmpty()) return;

        EstadoCita estadoCancelada = estadoCitaRepository.findByNombre(ESTADO_CANCELADA)
                .orElseThrow(() -> new IllegalStateException("Estado 'Cancelada' no configurado."));

        for (Cita cita : vencidas) {
            cita.setEstado(estadoCancelada);
        }
        citaRepository.saveAll(vencidas);
    }
}