package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.repository.MedicamentoRepository;

@Service
public class MedicamentoServiceImpl extends CatalogoServiceImpl<Medicamento> {
    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Override
    protected JpaRepository<Medicamento, Integer> getRepository(){
        return medicamentoRepository;
    }

    
}
