package org.umg.sistemamedicoii.config.cache;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EstadoCitaRepository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class EstadoCitaCache {

    @Autowired
    private EstadoCitaRepository repository;

    private final Map<EstadoCitaEnum, EstadoCita> cache = new EnumMap<>(EstadoCitaEnum.class);

    @PostConstruct
    public void init() {
        List<EstadoCita> estadosBd = repository.findAll();
        for (EstadoCitaEnum estadoEnum : EstadoCitaEnum.values()) {
            estadosBd.stream()
                    .filter(e -> e.getNombre().equalsIgnoreCase(estadoEnum.getNombreBd()))
                    .findFirst()
                    .ifPresent(e -> cache.put(estadoEnum, e));
        }
    }

    public EstadoCita getEstado(EstadoCitaEnum estadoEnum) {
        EstadoCita estado = cache.get(estadoEnum);
        if (estado == null) {
            throw new IllegalStateException("Estado crítico no configurado en BD: " + estadoEnum.getNombreBd());
        }
        return estado;
    }
}