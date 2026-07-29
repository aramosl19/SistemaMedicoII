package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.ExamenLaboratorio;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.ExamenLaboratorioRepository;

@Service
public class ExamenLaboratorioServiceImpl extends CatalogoServiceImpl<ExamenLaboratorio> implements ExamenLaboratorioService {

    @Autowired
    private ExamenLaboratorioRepository examenLaboratorioRepository;

    @Override
    protected CatalogoRepository<ExamenLaboratorio> getRepository() {
        return examenLaboratorioRepository;
    }
}