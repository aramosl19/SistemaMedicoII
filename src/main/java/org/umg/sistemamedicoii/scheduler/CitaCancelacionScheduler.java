package org.umg.sistemamedicoii.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.config.EstadoCitaCache;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.repository.CitaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CitaCancelacionScheduler {

    private static final int MINUTOS_LIMITE = 10;

    @Autowired private CitaRepository citaRepository;
    @Autowired private EstadoCitaCache estadoCache;

    @Scheduled(fixedRate = 60000)
    public void cancelarCitasPendientesVencidas() {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(MINUTOS_LIMITE);

        List<Cita> vencidas = citaRepository
                .findByEstado_NombreAndCreadaPorPersonalInternoFalseAndFechaCreacionBefore(
                        EstadoCitaEnum.PENDIENTE_PAGO.getNombreBd(), limite);

        if (vencidas.isEmpty()) return;

        for (Cita cita : vencidas) {
            cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.CANCELADA));
        }
        citaRepository.saveAll(vencidas);
    }
}