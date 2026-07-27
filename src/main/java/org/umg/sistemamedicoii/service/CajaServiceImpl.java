package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.CitaCobroResponseDTO;
import org.umg.sistemamedicoii.dto.CobroCajaRequestDTO;
import org.umg.sistemamedicoii.dto.CobroCajaResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.PagoEfectivoRepository;
import org.umg.sistemamedicoii.repository.PagoTarjetaRepository;
import org.umg.sistemamedicoii.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CajaServiceImpl implements CajaService {

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PagoTarjetaRepository pagoTarjetaRepository;
    @Autowired private PagoEfectivoRepository pagoEfectivoRepository;

    @Autowired private EstadoCitaCache estadoCache; // <-- Magia del caché
    @Autowired private EmailService emailService;

    @Autowired private List<ProcesadorPagoStrategy> estrategiasPago; // <-- Magia del Strategy

    @Override
    public List<CitaCobroResponseDTO> buscarCitasPendientes(Integer numeroCita, String dpi) {
        if (numeroCita == null && (dpi == null || dpi.isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un número de cita o DPI para buscar.");
        }

        List<Cita> citas = new ArrayList<>();
        String estadoPendiente = EstadoCitaEnum.PENDIENTE_PAGO.getNombreBd();

        if (numeroCita != null) {
            citaRepository.findByIdAndEstado_Nombre(numeroCita, estadoPendiente).ifPresent(citas::add);
        } else {
            usuarioRepository.findByDpi(dpi).ifPresent(paciente ->
                    citas.addAll(citaRepository.findByPaciente_IdAndEstado_NombreOrderByFechaHoraAsc(paciente.getId(), estadoPendiente)));
        }

        return citas.stream().map(this::toCobroDTO).toList();
    }

    @Override
    public CobroCajaResponseDTO procesarCobro(CobroCajaRequestDTO dto) {
        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontraron citas pendientes de pago para el criterio ingresado."));

        if (!EstadoCitaEnum.PENDIENTE_PAGO.getNombreBd().equalsIgnoreCase(cita.getEstado().getNombre())) {
            throw new IllegalArgumentException("No se encontraron citas pendientes de pago para el criterio ingresado.");
        }

        if (pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(TipoConceptoCobro.CITA, cita.getId()) ||
                pagoEfectivoRepository.existsByTipoConceptoAndReferenciaId(TipoConceptoCobro.CITA, cita.getId())) {
            throw new IllegalArgumentException("Esta cita ya fue cobrada.");
        }

        String numeroTransaccion = UUID.randomUUID().toString();

        // Ejecutamos la estrategia correcta automáticamente sin if/else
        ProcesadorPagoStrategy estrategia = estrategiasPago.stream()
                .filter(e -> e.soportaMetodo(dto.getMetodoPago()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El método de pago seleccionado no está disponible. Los métodos aceptados son: efectivo (Quetzales), tarjeta de crédito (Visa/Mastercard) o tarjeta de débito."));

        BigDecimal[] montos = estrategia.procesarPago(dto, cita.getPrecio(), cita.getId(), cita.getPaciente().getNombreCompleto(), numeroTransaccion);

        // Actualizamos estado sacándolo directamente del Caché (Sin ir a la BD)
        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.CONFIRMADA));
        cita.setReservadaHasta(null);
        citaRepository.save(cita);

        emailService.enviarCorreo(
                cita.getPaciente().getCorreo(),
                "Comprobante de Pago - Consulta Médica - " + nombreHospital,
                "Estimado(a) " + cita.getPaciente().getNombreCompleto() + ", su pago en caja fue registrado exitosamente. Número de transacción: " + numeroTransaccion + ". Su cita ha sido confirmada."
        );

        CobroCajaResponseDTO response = new CobroCajaResponseDTO();
        response.setNumeroTransaccion(numeroTransaccion);
        response.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        response.setMedicoNombre(cita.getMedico().getNombreCompleto());
        response.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        response.setSucursalNombre(cita.getSucursal().getNombre());
        response.setFechaHoraCita(cita.getFechaHora());
        response.setMonto(cita.getPrecio());
        response.setMetodoPago(dto.getMetodoPago().toUpperCase());
        response.setMontoRecibido(montos[0]);
        response.setCambio(montos[1]);
        response.setMensaje("¡Pago registrado exitosamente! Paciente: " + cita.getPaciente().getNombreCompleto() + ". La cita ha sido actualizada a estado Confirmada.");
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
        dto.setMonto(cita.getPrecio());
        return dto;
    }
}