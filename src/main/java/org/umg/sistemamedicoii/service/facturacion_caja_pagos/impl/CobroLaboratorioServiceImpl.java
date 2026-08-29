package org.umg.sistemamedicoii.service.facturacion_caja_pagos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.examenes_laboratorio.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.OrdenLaboratorioRepository;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.ProcesadorPagoStrategy;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.CobroLaboratorioService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CobroLaboratorioServiceImpl implements CobroLaboratorioService {

    @Autowired private OrdenLaboratorioRepository ordenLaboratorioRepository;
    @Autowired private List<ProcesadorPagoStrategy> estrategiasPago;

    @Override
    public CobroLaboratorioResponseDTO cobrar(CobroLaboratorioRequestDTO dto) {
        OrdenLaboratorio orden = ordenLaboratorioRepository.findById(dto.getOrdenId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la orden de laboratorio ingresada."));

        if (orden.getEstado() != EstadoOrdenLaboratorioEnum.PENDIENTE) {
            throw new IllegalArgumentException(
                    "No es posible cobrar esta orden: se encuentra en estado '" + orden.getEstado().getNombre() + "'.");
        }

        ProcesadorPagoStrategy estrategia = estrategiasPago.stream()
                .filter(e -> e.soportaMetodo(dto.getMetodoPago()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "El método de pago seleccionado no está disponible. Los métodos aceptados son: efectivo (Quetzales), tarjeta de crédito (Visa/Mastercard) o tarjeta de débito."));

        String numeroTransaccion = UUID.randomUUID().toString();
        String nombreTitular = orden.getCita().getPaciente().getNombreCompleto();

        BigDecimal[] montos = estrategia.procesarPago(
                dto, orden.getMontoTotal(), orden.getId(), nombreTitular, numeroTransaccion, TipoConceptoCobro.LABORATORIO);

        orden.setEstado(EstadoOrdenLaboratorioEnum.EN_PROCESO);
        ordenLaboratorioRepository.save(orden);

        CobroLaboratorioResponseDTO respuesta = new CobroLaboratorioResponseDTO();
        respuesta.setNumeroTransaccion(numeroTransaccion);
        respuesta.setOrdenId(orden.getId());
        respuesta.setPacienteNombre(nombreTitular);
        // No existe relación OrdenLaboratorio -> laboratorista/supervisor;
        // se toma la sucursal de la cita de origen (Cita ya trae su propia sucursal).
        respuesta.setSucursal(orden.getCita().getSucursal().getNombre());
        respuesta.setMonto(orden.getMontoTotal());
        respuesta.setCantidadExamenes(orden.getDetalles().size());
        respuesta.setMetodoPago(dto.getMetodoPago());
        respuesta.setMontoRecibido(montos[0]);
        respuesta.setCambio(montos[1]);

        // Solución CU-10: Mensaje exacto de éxito incluyendo el paciente
        respuesta.setMensaje("¡Pago de laboratorio registrado exitosamente! Paciente: " + nombreTitular + ". La orden ha sido actualizada a estado 'En proceso'.");
        return respuesta;
    }

    @Override
    public List<OrdenLaboratorioResponseDTO> buscarOrdenesPendientes(Integer numeroOrden, String dpi) {
        if (numeroOrden == null && (dpi == null || dpi.isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un número de orden o DPI para buscar.");
        }
        List<OrdenLaboratorio> ordenes = new java.util.ArrayList<>();
        if (numeroOrden != null) {
            ordenLaboratorioRepository.findById(numeroOrden)
                    .filter(o -> o.getEstado() == EstadoOrdenLaboratorioEnum.PENDIENTE)
                    .ifPresent(ordenes::add);
        } else {
            // Consulta directa a base de datos, evita NullPointerExceptions de Java y es escalable
            ordenes = ordenLaboratorioRepository.findByEstadoAndCita_Paciente_DpiOrderByFechaCreacionAsc(EstadoOrdenLaboratorioEnum.PENDIENTE, dpi);
        }

        return ordenes.stream().map(o -> {
            OrdenLaboratorioResponseDTO dto = new OrdenLaboratorioResponseDTO();
            dto.setId(o.getId());
            dto.setCitaId(o.getCita().getId());
            dto.setPacienteNombre(o.getCita().getPaciente().getNombreCompleto());
            // El DPI ya viaja encriptado en BD (CryptoConverter en Usuario.dpi);
            // JPA lo desencripta solo al leerlo, no requiere manejo adicional aquí.
            dto.setPacienteDpi(o.getCita().getPaciente().getDpi());
            dto.setCantidadExamenes(o.getDetalles().size());
            dto.setMedicoNombre(o.getMedico().getNombreCompleto());
            dto.setEspecialidadNombre(o.getCita().getEspecialidad().getNombre());
            dto.setFechaHora(o.getCita().getFechaHora());
            dto.setEstado(o.getEstado().getNombre());
            dto.setMontoTotal(o.getMontoTotal());
            dto.setFechaCreacion(o.getFechaCreacion());
            return dto;
        }).toList();
    }
}