package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.EstadoCita;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.EstadoCitaRepository;

@Service
public class EstadoCitaServiceImpl extends CatalogoServiceImpl<EstadoCita>{

    @Autowired
    private EstadoCitaRepository estadoCitaRepository;

    @Override
    protected CatalogoRepository<EstadoCita> getRepository(){
        return estadoCitaRepository;
    }
}
