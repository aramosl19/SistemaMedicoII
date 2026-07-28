package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.CobroLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.CobroLaboratorioResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoOrdenLaboratorioEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.OrdenLaboratorio;
import org.umg.sistemamedicoii.repository.OrdenLaboratorioRepository;

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
        respuesta.setMonto(orden.getMontoTotal());
        respuesta.setMetodoPago(dto.getMetodoPago());
        respuesta.setMontoRecibido(montos[0]);
        respuesta.setCambio(montos[1]);
        respuesta.setMensaje("Cobro de laboratorio registrado exitosamente. La orden queda habilitada para la toma de muestras.");
        return respuesta;
    }
}