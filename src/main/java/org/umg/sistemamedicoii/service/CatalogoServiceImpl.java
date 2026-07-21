package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.models.Catalogo;
import org.umg.sistemamedicoii.repository.CatalogoRepository;

import java.util.List;

public abstract class CatalogoServiceImpl<T extends Catalogo> implements CatalogoService<T> {

    protected abstract CatalogoRepository<T> getRepository();

    @Override
    public List<T> listar() {
        return getRepository().findAll();
    }

    @Override
    public T obtenerPorId(Integer id) {
        return getRepository().findById(id)
                .orElseThrow(() -> new RuntimeException("Registro con id " + id + " no encontrado"));
    }

    @Override
    public T crear(T entidad) {
        if (!getRepository().findByNombreIgnoreCaseAndActivoTrue(entidad.getNombre()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Ya existe un registro con el nombre " + entidad.getNombre() + " en este catálogo.");
        }
        return getRepository().save(entidad);
    }

    @Override
    public T actualizar(Integer id, T entidad) {
        obtenerPorId(id);
        entidad.setId(id);
        return getRepository().save(entidad);
    }

    @Override
    public void eliminar(Integer id) {
        getRepository().deleteById(id);
    }
}