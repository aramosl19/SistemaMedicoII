package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.CobroCajaResponseDTO;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.*;
import org.umg.sistemamedicoii.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CajaServiceImpl implements CajaService {

    private static final String ESTADO_PENDIENTE_PAGO = "Pendiente de pago";
    private static final String ESTADO_CONFIRMADA = "Confirmada";
    private static final Set<String> METODOS_TARJETA = Set.of("VISA", "MASTERCARD", "DEBITO");
    private static final String TARJETA_RECHAZO_SIMULADO = "0000";

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PagoTarjetaRepository pagoTarjetaRepository;
    @Autowired private PagoEfectivoRepository pagoEfectivoRepository;
    @Autowired private EstadoCitaRepository estadoCitaRepository;
    @Autowired private EmailService emailService;

    @Override
    public List<CitaCobroResponseDTO> buscarCitasPendientes(Integer numeroCita, String dpi) {
        if (numeroCita == null && (dpi == null || dpi.isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un número de cita o DPI para buscar.");
        }

        List<Cita> citas = new ArrayList<>();

        if (numeroCita != null) {
            citaRepository.findByIdAndEstado_Nombre(numeroCita, ESTADO_PENDIENTE_PAGO)
                    .ifPresent(citas::add);
        } else {
            usuarioRepository.findByDpi(dpi).ifPresent(paciente ->
                    citas.addAll(citaRepository.findByPaciente_IdAndEstado_NombreOrderByFechaHoraAsc(
                            paciente.getId(), ESTADO_PENDIENTE_PAGO)));
        }

        List<CitaCobroResponseDTO> resultado = new ArrayList<>();
        for (Cita cita : citas) {
            resultado.add(toCobroDTO(cita));
        }
        return resultado;
    }

    @Override
    public CobroCajaResponseDTO procesarCobro(CobroCajaRequestDTO dto) {
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontraron citas pendientes de pago para el criterio ingresado."));

        if (!ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(cita.getEstado().getNombre())) {
            throw new IllegalArgumentException(
                    "No se encontraron citas pendientes de pago para el criterio ingresado.");
        }

        if (pagoTarjetaRepository.existsByCitaId(cita.getId()) || pagoEfectivoRepository.existsByCitaId(cita.getId())) {
            throw new IllegalArgumentException("Esta cita ya fue cobrada.");
        }

        String metodo = dto.getMetodoPago().trim().toUpperCase();
        BigDecimal monto = cita.getEspecialidad().getPrecio();
        String numeroTransaccion = UUID.randomUUID().toString();
        BigDecimal montoRecibido = null;
        BigDecimal cambio = null;

        if ("EFECTIVO".equals(metodo)) {
            if (dto.getMontoRecibido() == null) {
                throw new IllegalArgumentException("Debe ingresar el monto recibido.");
            }
            if (dto.getMontoRecibido().compareTo(monto) < 0) {
                throw new IllegalArgumentException(
                        "El monto recibido (Q" + dto.getMontoRecibido() + ") es menor al monto a cobrar (Q" + monto + ")");
            }
            montoRecibido = dto.getMontoRecibido();
            cambio = montoRecibido.subtract(monto);

            PagoEfectivo pago = new PagoEfectivo();
            pago.setCita(cita);
            pago.setNumeroTransaccion(numeroTransaccion);
            pago.setMonto(monto);
            pago.setMontoRecibido(montoRecibido);
            pago.setCambio(cambio);
            pago.setFechaPago(LocalDateTime.now());
            pagoEfectivoRepository.save(pago);

        } else if (METODOS_TARJETA.contains(metodo)) {
            if (dto.getUltimosCuatroDigitos() == null || !dto.getUltimosCuatroDigitos().matches("\\d{4}")) {
                throw new IllegalArgumentException("Ingrese los últimos 4 dígitos de la tarjeta.");
            }
            if (TARJETA_RECHAZO_SIMULADO.equals(dto.getUltimosCuatroDigitos())) {
                throw new PagoRechazadoException(
                        "La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.");
            }

            PagoTarjeta pago = new PagoTarjeta();
            pago.setCita(cita);
            pago.setNumeroTransaccion(numeroTransaccion);
            pago.setMonto(monto);
            pago.setUltimosCuatroDigitos(dto.getUltimosCuatroDigitos());
            pago.setNombreTitular(cita.getPaciente().getNombreCompleto().toUpperCase());
            pago.setFechaPago(LocalDateTime.now());
            pagoTarjetaRepository.save(pago);

        } else {
            throw new IllegalArgumentException(
                    "El método de pago seleccionado no está disponible. Los métodos aceptados son: efectivo (Quetzales), " +
                            "tarjeta de crédito (Visa/Mastercard) o tarjeta de débito.");
        }

        EstadoCita estadoConfirmada = estadoCitaRepository.findByNombre(ESTADO_CONFIRMADA)
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'Confirmada' no configurado."));
        cita.setEstado(estadoConfirmada);
        cita.setReservadaHasta(null);
        citaRepository.save(cita);

        emailService.enviarCorreo(
                cita.getPaciente().getCorreo(),
                "Comprobante de Pago - Consulta Médica - " + nombreHospital,
                "Estimado(a) " + cita.getPaciente().getNombreCompleto() + ", su pago en caja fue registrado exitosamente. " +
                        "Número de transacción: " + numeroTransaccion + ". Su cita ha sido confirmada."
        );

        CobroCajaResponseDTO response = new CobroCajaResponseDTO();
        response.setNumeroTransaccion(numeroTransaccion);
        response.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        response.setMedicoNombre(cita.getMedico().getNombreCompleto());
        response.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        response.setSucursalNombre(cita.getSucursal().getNombre());
        response.setFechaHoraCita(cita.getFechaHora());
        response.setMonto(monto);
        response.setMetodoPago(metodo);
        response.setMontoRecibido(montoRecibido);
        response.setCambio(cambio);
        response.setMensaje("¡Pago registrado exitosamente! Paciente: " + cita.getPaciente().getNombreCompleto() +
                ". La cita ha sido actualizada a estado Confirmada.");
        return response;
    }

    private CitaCobroResponseDTO toCobroDTO(Cita cita) {
        CitaCobroResponseDTO dto = new CitaCobroResponseDTO();
        dto.setId(cita.getId());
        dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        dto.setPacienteDpi(cita.getPaciente().getDpi());
        dto.setMedicoNombre(cita.getMedico().getNombreCompleto());
        dto.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        dto.setSucursalNombre(cita.getSucursal().getNombre());
        dto.setFechaHora(cita.getFechaHora());
        dto.setMonto(cita.getEspecialidad().getPrecio());
        return dto;
    }
}