package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.umg.sistemamedicoii.dto.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.MovimientoInventarioResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.InventarioMedicamento;
import org.umg.sistemamedicoii.models.Medicamento;
import org.umg.sistemamedicoii.models.MovimientoInventario;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.repository.InventarioMedicamentoRepository;
import org.umg.sistemamedicoii.repository.MedicamentoRepository;
import org.umg.sistemamedicoii.repository.MovimientoInventarioRepository;
import org.umg.sistemamedicoii.repository.SucursalRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovimientoInventarioServiceImpl implements MovimientoInventarioService {

    @Autowired private MovimientoInventarioRepository movimientoRepository;
    @Autowired private InventarioMedicamentoRepository inventarioRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;
    @Autowired private SucursalRepository sucursalRepository;

    @Override
    public List<MovimientoInventarioResponseDTO> listar() {
        return movimientoRepository.findAllByOrderByFechaHoraDesc().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional // Para manejar el Bloqueo Optimista correctamente
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioRequestDTO dto) {

        validarReglasDeNegocio(dto);

        Medicamento medicamento = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));

        // Obtener inventario actual o crearlo si no existe (inicialización en 0)
        InventarioMedicamento inv = inventarioRepository.findByMedicamentoIdAndSucursalId(medicamento.getId(), sucursal.getId())
                .orElseGet(() -> {
                    InventarioMedicamento nuevo = new InventarioMedicamento();
                    nuevo.setMedicamento(medicamento);
                    nuevo.setSucursal(sucursal);
                    nuevo.setStockActual(0);
                    return nuevo;
                });

        int stockAnterior = inv.getStockActual();
        int stockNuevo;
        boolean esEntrada = esMovimientoEntrada(dto.getTipoMovimiento());

        if (esEntrada) {
            stockNuevo = stockAnterior + dto.getCantidad();
        } else {
            // RN-CU13-02: Validación de Stock Suficiente para salidas
            if (stockAnterior < dto.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + stockAnterior + ". No se puede registrar una salida de " + dto.getCantidad() + " unidades.");
            }
            stockNuevo = stockAnterior - dto.getCantidad();
        }

        inv.setStockActual(stockNuevo);
        inventarioRepository.save(inv); // Si otro usuario editó esto al mismo tiempo, lanzará OptimisticLockingFailureException

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setTipoMovimiento(dto.getTipoMovimiento());
        movimiento.setMedicamento(medicamento);
        movimiento.setSucursal(sucursal);
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockNuevo(stockNuevo);
        movimiento.setCostoUnitario(dto.getCostoUnitario());
        movimiento.setReferencia(dto.getReferencia());
        movimiento.setMotivo(dto.getMotivo());
        movimiento.setFechaHora(LocalDateTime.now());

        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        return toResponseDTO(guardado);
    }

    private void validarReglasDeNegocio(MovimientoInventarioRequestDTO dto) {
        // RN-CU13-01: Validaciones Dinámicas
        if (dto.getTipoMovimiento() == 6) {
            throw new IllegalArgumentException("El movimiento tipo 'Despacho (6)' es automático y no puede crearse manualmente.");
        }

        if (dto.getTipoMovimiento() == 0) { // Compra
            if (dto.getCostoUnitario() == null || dto.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El costo unitario es obligatorio para compras y debe ser mayor a 0.");
            }
        }

        if (List.of(1, 3, 4, 5).contains(dto.getTipoMovimiento())) { // Devolución, Reclamo, Ajuste+, Ajuste-
            if (dto.getMotivo() == null || dto.getMotivo().length() < 10 || dto.getMotivo().length() > 500) {
                throw new IllegalArgumentException("El motivo debe contener entre 10 y 500 caracteres para este tipo de movimiento.");
            }
        }
    }

    private boolean esMovimientoEntrada(int tipo) {
        // 0=Compra, 1=Devolución, 4=Ajuste+
        return tipo == 0 || tipo == 1 || tipo == 4;
    }

    private String obtenerNombreMovimiento(int tipo) {
        return switch (tipo) {
            case 0 -> "Compra";
            case 1 -> "Devolución";
            case 2 -> "Venta";
            case 3 -> "Reclamo";
            case 4 -> "Ajuste (+)";
            case 5 -> "Ajuste (-)";
            case 6 -> "Despacho Automático";
            default -> "Desconocido";
        };
    }

    private MovimientoInventarioResponseDTO toResponseDTO(MovimientoInventario mov) {
        MovimientoInventarioResponseDTO dto = new MovimientoInventarioResponseDTO();
        dto.setId(mov.getId());
        dto.setTipoMovimientoNombre(obtenerNombreMovimiento(mov.getTipoMovimiento()));
        dto.setMedicamentoNombre(mov.getMedicamento().getNombre());
        dto.setSucursalNombre(mov.getSucursal().getNombre());
        dto.setCantidad(mov.getCantidad());
        dto.setStockAnterior(mov.getStockAnterior());
        dto.setStockNuevo(mov.getStockNuevo());
        dto.setCostoUnitario(mov.getCostoUnitario());
        dto.setReferencia(mov.getReferencia());
        dto.setMotivo(mov.getMotivo());
        dto.setFechaHora(mov.getFechaHora());
        return dto;
    }
}