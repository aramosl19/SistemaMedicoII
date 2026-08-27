package org.umg.sistemamedicoii.service.examenes_laboratorio.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.examenes_laboratorio.ExamenLaboratorio;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.ExamenLaboratorioRepository;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl.CatalogoServiceImpl;
import org.umg.sistemamedicoii.service.examenes_laboratorio.ExamenLaboratorioService;

@Service
public class ExamenLaboratorioServiceImpl extends CatalogoServiceImpl<ExamenLaboratorio> implements ExamenLaboratorioService {

    @Autowired
    private ExamenLaboratorioRepository examenLaboratorioRepository;

    @Override
    protected CatalogoRepository<ExamenLaboratorio> getRepository() {
        return examenLaboratorioRepository;
    }
}