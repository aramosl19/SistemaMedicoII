package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.TipoCita;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.TipoCitaRepository;

@Service
public class TipoCitaServiceImpl extends CatalogoServiceImpl<TipoCita> {

    @Autowired
    private TipoCitaRepository tipoCitaRepository;

    @Override
    protected CatalogoRepository<TipoCita> getRepository() {
        return tipoCitaRepository;
    }
}