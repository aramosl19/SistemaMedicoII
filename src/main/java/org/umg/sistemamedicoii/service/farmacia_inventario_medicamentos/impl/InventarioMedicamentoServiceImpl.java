package org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.farmacia_inventario_medicamentos.InventarioMedicamentoResponseDTO;
import org.umg.sistemamedicoii.models.farmacia_inventario_medicamentos.InventarioMedicamento;
import org.umg.sistemamedicoii.repository.farmacia_inventario_medicamentos.InventarioMedicamentoRepository;
import org.umg.sistemamedicoii.service.farmacia_inventario_medicamentos.InventarioMedicamentoService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventarioMedicamentoServiceImpl implements InventarioMedicamentoService {

    @Autowired
    private InventarioMedicamentoRepository inventarioRepository;

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

    private InventarioMedicamentoResponseDTO toResponseDTO(InventarioMedicamento inv) {
        InventarioMedicamentoResponseDTO dto = new InventarioMedicamentoResponseDTO();
        dto.setId(inv.getId());
        dto.setMedicamentoId(inv.getMedicamento().getId());
        dto.setMedicamentoNombre(inv.getMedicamento().getNombre());
        dto.setSucursalNombre(inv.getSucursal().getNombre());
        dto.setStockActual(inv.getStockActual());

        Integer minStock = inv.getMedicamento().getMinimumStock();
        dto.setStockMinimo(minStock);
        dto.setMedicamentoControlado(inv.getMedicamento().isControlled());

        // RN-CU10-03 y CU-14: Lógica de alerta de stock bajo
        dto.setAlertaStockBajo(minStock != null && inv.getStockActual() <= minStock);

        return dto;
    }
}