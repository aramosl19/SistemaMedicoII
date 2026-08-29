package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoRequestDTO;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoResponseDTO;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.InventarioMedicamento;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.Medicamento;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.InventarioMedicamentoRepository;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.InventarioMedicamentoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventarioMedicamentoServiceImpl implements InventarioMedicamentoService {

    @Autowired
    private InventarioMedicamentoRepository inventarioRepository;
    @Autowired
    private MedicamentoRepository medicamentoRepository;
    @Autowired
    private SucursalRepository sucursalRepository;

    @Override
    public List<InventarioMedicamentoResponseDTO> listarInventarioPorSucursal(Integer sucursalId) {
        return inventarioRepository.findAll().stream()
                .filter(inv -> inv.getSucursal().getId().equals(sucursalId) && inv.isActivo())
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventarioMedicamentoResponseDTO> listarTodo() {
        return inventarioRepository.findAll().stream()
                .filter(InventarioMedicamento::isActivo)
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public InventarioMedicamentoResponseDTO crear(InventarioMedicamentoRequestDTO dto) {
        if (inventarioRepository.findByMedicamentoIdAndSucursalId(dto.getMedicamentoId(), dto.getSucursalId())
                .filter(InventarioMedicamento::isActivo).isPresent()) {
            throw new DuplicateResourceException("Ya existe un registro de inventario para ese medicamento en esa sede.");
        }

        InventarioMedicamento inv = inventarioRepository
                .findByMedicamentoIdAndSucursalId(dto.getMedicamentoId(), dto.getSucursalId())
                .orElseGet(InventarioMedicamento::new); // Reutiliza la fila si existía inactiva

        inv.setMedicamento(buscarMedicamento(dto.getMedicamentoId()));
        inv.setSucursal(buscarSucursal(dto.getSucursalId()));
        inv.setStockActual(dto.getStockActual());
        inv.setActivo(true);

        return toResponseDTO(inventarioRepository.save(inv));
    }

    @Override
    public InventarioMedicamentoResponseDTO actualizar(Integer id, InventarioMedicamentoRequestDTO dto) {
        InventarioMedicamento inv = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de inventario con id " + id + " no encontrado."));

        // El par medicamento/sede identifica el registro; no se reasigna en edición,
        // solo se ajusta el stock. Si mandan un par distinto, lo tratamos como error de uso.
        if (!inv.getMedicamento().getId().equals(dto.getMedicamentoId())
                || !inv.getSucursal().getId().equals(dto.getSucursalId())) {
            throw new IllegalArgumentException("No se puede reasignar el medicamento o la sede de un registro existente.");
        }

        inv.setStockActual(dto.getStockActual());
        if (dto.getActivo() != null) inv.setActivo(dto.getActivo());

        return toResponseDTO(inventarioRepository.save(inv));
    }

    @Override
    public void eliminar(Integer id) {
        InventarioMedicamento inv = inventarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de inventario con id " + id + " no encontrado."));
        inv.setActivo(false);
        inventarioRepository.save(inv);
    }

    private Medicamento buscarMedicamento(Integer id) {
        return medicamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento con id " + id + " no encontrado."));
    }

    private Sucursal buscarSucursal(Integer id) {
        return sucursalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal con id " + id + " no encontrada."));
    }

    private InventarioMedicamentoResponseDTO toResponseDTO(InventarioMedicamento inv) {
        InventarioMedicamentoResponseDTO dto = new InventarioMedicamentoResponseDTO();
        dto.setId(inv.getId());
        dto.setMedicamentoId(inv.getMedicamento().getId());
        dto.setMedicamentoNombre(inv.getMedicamento().getNombre());
        dto.setSucursalId(inv.getSucursal().getId());
        dto.setSucursalNombre(inv.getSucursal().getNombre());
        dto.setStockActual(inv.getStockActual());
        dto.setActivo(inv.isActivo());

        Integer minStock = inv.getMedicamento().getMinimumStock();
        dto.setStockMinimo(minStock);
        dto.setMedicamentoControlado(inv.getMedicamento().isControlled());
        dto.setAlertaStockBajo(minStock != null && inv.getStockActual() <= minStock);

        return dto;
    }
}