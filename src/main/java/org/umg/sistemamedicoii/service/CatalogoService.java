package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.models.Catalogo;

import java.util.List;

public interface CatalogoService<T extends Catalogo> {
    List<T> listar();
    T obtenerPorId(Integer id);
    T crear(T entidad);
    T actualizar(Integer id, T entidad);
    void eliminar(Integer id);
}
