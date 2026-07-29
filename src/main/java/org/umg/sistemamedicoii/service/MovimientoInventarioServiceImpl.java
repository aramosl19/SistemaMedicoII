package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.umg.sistemamedicoii.dto.MovimientoInventarioRequestDTO;
import org.umg.sistemamedicoii.dto.MovimientoInventarioResponseDTO;
import org.umg.sistemamedicoii.dto.ResumenMensualInventarioResponseDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @Transactional
    public MovimientoInventarioResponseDTO registrarMovimiento(MovimientoInventarioRequestDTO dto) {

        validarReglasDeNegocio(dto);

        Medicamento medicamento = medicamentoRepository.findById(dto.getMedicamentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento no encontrado."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));

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
            if (stockAnterior < dto.getCantidad()) {
                throw new IllegalArgumentException("Stock insuficiente. Stock actual: " + stockAnterior + ". No se puede registrar una salida de " + dto.getCantidad() + " unidades.");
            }
            stockNuevo = stockAnterior - dto.getCantidad();
        }

        inv.setStockActual(stockNuevo);
        inventarioRepository.save(inv);

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

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
            movimiento.setUsuarioId(principal.getUsuario().getId());
        }

        movimiento.setFechaHora(LocalDateTime.now());

        MovimientoInventario guardado = movimientoRepository.save(movimiento);

        return toResponseDTO(guardado);
    }

    @Override
    public List<ResumenMensualInventarioResponseDTO> generarResumenMensual(Integer sucursalId, int anio, int mes) {
        LocalDateTime desde = LocalDateTime.of(anio, mes, 1, 0, 0);
        LocalDateTime hasta = desde.plusMonths(1);

        List<MovimientoInventario> movimientos = movimientoRepository
                .findBySucursalIdAndFechaHoraBetweenOrderByFechaHoraAsc(sucursalId, desde, hasta);

        Map<Integer, List<MovimientoInventario>> agrupadosPorMedicamento = movimientos.stream()
                .collect(Collectors.groupingBy(m -> m.getMedicamento().getId()));

        List<ResumenMensualInventarioResponseDTO> resumenList = new ArrayList<>();

        for (Map.Entry<Integer, List<MovimientoInventario>> entry : agrupadosPorMedicamento.entrySet()) {
            List<MovimientoInventario> movs = entry.getValue();
            if (movs.isEmpty()) continue;

            MovimientoInventario primerMovimiento = movs.get(0);
            MovimientoInventario ultimoMovimiento = movs.get(movs.size() - 1);

            int totalEntradas = 0;
            int totalSalidas = 0;

            for (MovimientoInventario m : movs) {
                if (esMovimientoEntrada(m.getTipoMovimiento())) {
                    totalEntradas += m.getCantidad();
                } else {
                    totalSalidas += m.getCantidad();
                }
            }

            ResumenMensualInventarioResponseDTO resumen = new ResumenMensualInventarioResponseDTO();
            resumen.setMedicamentoId(primerMovimiento.getMedicamento().getId());
            resumen.setMedicamentoNombre(primerMovimiento.getMedicamento().getNombre());
            resumen.setTotalEntradas(totalEntradas);
            resumen.setTotalSalidas(totalSalidas);
            resumen.setStockInicial(primerMovimiento.getStockAnterior());
            resumen.setStockFinal(ultimoMovimiento.getStockNuevo());
            resumen.setCantidadMovimientos(movs.size());

            resumenList.add(resumen);
        }

        return resumenList;
    }

    private void validarReglasDeNegocio(MovimientoInventarioRequestDTO dto) {
        if (dto.getTipoMovimiento() == 6) {
            throw new IllegalArgumentException("El movimiento tipo 'Despacho (6)' es automático y no puede crearse manualmente.");
        }

        if (dto.getTipoMovimiento() == 0) {
            if (dto.getCostoUnitario() == null || dto.getCostoUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El costo unitario es obligatorio para compras y debe ser mayor a 0.");
            }
        }

        if (List.of(1, 3, 4, 5).contains(dto.getTipoMovimiento())) {
            if (dto.getMotivo() == null || dto.getMotivo().length() < 10 || dto.getMotivo().length() > 500) {
                throw new IllegalArgumentException("El motivo debe contener entre 10 y 500 caracteres para este tipo de movimiento.");
            }
        }
    }

    private boolean esMovimientoEntrada(int tipo) {
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