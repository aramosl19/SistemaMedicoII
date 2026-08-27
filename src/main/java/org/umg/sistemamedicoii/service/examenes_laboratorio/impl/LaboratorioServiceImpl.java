package org.umg.sistemamedicoii.service.examenes_laboratorio.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.examenes_laboratorio.Laboratorio;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.LaboratorioRepository;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl.CatalogoServiceImpl;

@Service
public class LaboratorioServiceImpl extends CatalogoServiceImpl<Laboratorio> {

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Override
    protected CatalogoRepository<Laboratorio> getRepository() {
        return laboratorioRepository;
    }
}
