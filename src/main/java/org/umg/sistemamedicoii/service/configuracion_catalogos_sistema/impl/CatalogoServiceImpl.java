package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Catalogo;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.CatalogoRepository;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.CatalogoService;

import java.util.List;

public abstract class CatalogoServiceImpl<T extends Catalogo> implements CatalogoService<T> {

    protected abstract CatalogoRepository<T> getRepository();

    @Override
    @Cacheable(value = "catalogos", key = "#root.targetClass.simpleName + '_lista'")
    public List<T> listar() {
        return getRepository().findAll();
    }

    @Override
    @Cacheable(value = "catalogos", key = "#root.targetClass.simpleName + '_' + #id")
    public T obtenerPorId(Integer id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro con id " + id + " no encontrado"));
    }

    @Override
    @CacheEvict(value = "catalogos", allEntries = true)
    public T crear(T entidad) {
        if (!getRepository().findByNombreIgnoreCaseAndActivoTrue(entidad.getNombre()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ya existe un registro con el nombre " + entidad.getNombre() + " en este catálogo.");
        }
        return getRepository().save(entidad);
    }

    @Override
    @CacheEvict(value = "catalogos", allEntries = true)
    public T actualizar(Integer id, T entidad) {
        obtenerPorId(id);

        boolean nombreDuplicado = getRepository()
                .findByNombreIgnoreCaseAndActivoTrue(entidad.getNombre())
                .stream()
                .anyMatch(otro -> !otro.getId().equals(id));

        if (nombreDuplicado) {
            throw new IllegalArgumentException(
                    "Ya existe un registro con el nombre " + entidad.getNombre() + " en este catálogo.");
        }

        entidad.setId(id);
        return getRepository().save(entidad);
    }

    @Override
    @CacheEvict(value = "catalogos", allEntries = true)
    public void eliminar(Integer id) {
        T entidad = obtenerPorId(id);
        entidad.setActivo(false);
        getRepository().save(entidad);
    }
}