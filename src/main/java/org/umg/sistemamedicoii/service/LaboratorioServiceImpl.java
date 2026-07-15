package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.Laboratorio;
import org.umg.sistemamedicoii.repository.LaboratorioRepository;

@Service
public class LaboratorioServiceImpl extends CatalogoServiceImpl<Laboratorio> {

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @Override
    protected JpaRepository<Laboratorio, Integer> getRepository() {
        return laboratorioRepository;
    }
}
