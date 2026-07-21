package org.umg.sistemamedicoii.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.SucursalRepository;

@Service
public class SucursalServiceImpl extends CatalogoServiceImpl<Sucursal>{

    @Autowired
    private SucursalRepository sucursalRepository;

    @Override
    protected CatalogoRepository<Sucursal> getRepository() {
        return sucursalRepository;
    }
}
