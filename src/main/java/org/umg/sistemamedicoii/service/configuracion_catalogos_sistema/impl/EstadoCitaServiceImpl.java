package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EstadoCitaRepository;

@Service
public class EstadoCitaServiceImpl extends CatalogoServiceImpl<EstadoCita> {

    @Autowired
    private EstadoCitaRepository estadoCitaRepository;

    @Override
    protected CatalogoRepository<EstadoCita> getRepository(){
        return estadoCitaRepository;
    }
}
