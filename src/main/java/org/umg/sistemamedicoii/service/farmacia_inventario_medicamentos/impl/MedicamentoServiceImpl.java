package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.Medicamento;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.MedicamentoRepository;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl.CatalogoServiceImpl;

@Service
public class MedicamentoServiceImpl extends CatalogoServiceImpl<Medicamento> {
    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Override
    protected CatalogoRepository<Medicamento> getRepository(){
        return medicamentoRepository;
    }

    
}
