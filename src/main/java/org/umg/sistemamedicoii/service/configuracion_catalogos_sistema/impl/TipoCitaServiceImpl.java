package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.TipoCita;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.TipoCitaRepository;

@Service
public class TipoCitaServiceImpl extends CatalogoServiceImpl<TipoCita> {

    @Autowired
    private TipoCitaRepository tipoCitaRepository;

    @Override
    protected CatalogoRepository<TipoCita> getRepository() {
        return tipoCitaRepository;
    }
}