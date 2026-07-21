package org.umg.sistemamedicoii.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.models.Rol;
import org.umg.sistemamedicoii.repository.CatalogoRepository;
import org.umg.sistemamedicoii.repository.RolRepository;

@Service
public class RolServiceImpl extends CatalogoServiceImpl<Rol> {

    @Autowired
    private RolRepository rolRepository;

    @Override
    protected CatalogoRepository<Rol> getRepository() {
        return rolRepository;
    }
}