package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EspecialidadRepository;

@Service
public class EspecialidadServiceImpl extends CatalogoServiceImpl<Especialidad> {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Override
    protected CatalogoRepository<Especialidad> getRepository(){
        return especialidadRepository; }
}
