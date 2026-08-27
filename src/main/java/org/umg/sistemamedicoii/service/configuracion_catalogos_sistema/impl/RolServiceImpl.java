package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Rol;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.RolRepository;

@Service
public class RolServiceImpl extends CatalogoServiceImpl<Rol> {

    @Autowired
    private RolRepository rolRepository;

    @Override
    protected CatalogoRepository<Rol> getRepository() {
        return rolRepository;
    }
}