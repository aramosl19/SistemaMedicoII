package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.Especialidad;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.EspecialidadRepository;

@Service
public class EspecialidadServiceImpl extends CatalogoServiceImpl<Especialidad>{

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Override
    protected CatalogoRepository<Especialidad> getRepository(){
        return especialidadRepository; }
}
