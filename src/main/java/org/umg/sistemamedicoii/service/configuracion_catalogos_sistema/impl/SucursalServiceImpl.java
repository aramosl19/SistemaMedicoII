package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;

@Service
public class SucursalServiceImpl extends CatalogoServiceImpl<Sucursal> {

    @Autowired
    private SucursalRepository sucursalRepository;

    @Override
    protected CatalogoRepository<Sucursal> getRepository() {
        return sucursalRepository;
    }
}
