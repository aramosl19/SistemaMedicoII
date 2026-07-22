package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.PagoResponseDTO;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.*;
import org.umg.sistemamedicoii.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
public class PagoServiceImpl implements PagoService {

    // --- Tarjetas de prueba fijas (simulan la pasarela) ---
    private static final String TARJETA_FONDOS_INSUFICIENTES = "4000000000000200";
    private static final String TARJETA_ERROR_COMUNICACION = "4000000000000309";

    @Autowired private CitaRepository citaRepository;
    @Autowired private PagoRepository pagoRepository;
    @Autowired private EstadoCitaRepository estadoCitaRepository;
    @Autowired private EmailService emailService;

    @Override
    public PagoResponseDTO procesarPago(PagoRequestDTO dto) {

        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        if (cita.getReservadaHasta() != null && cita.getReservadaHasta().isBefore(LocalDateTime.now())) {
            citaRepository.delete(cita);
            throw new IllegalArgumentException(
                    "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. Por favor, seleccione un nuevo horario.");
        }

        if (pagoRepository.existsByCitaId(cita.getId())) {
            throw new IllegalArgumentException("Esta cita ya fue pagada.");
        }

        if (!esLuhnValido(dto.getNumeroTarjeta())) {
            throw new IllegalArgumentException("El número de tarjeta no es válido.");
        }

        String[] partesVencimiento = dto.getVencimiento().split("/");
        YearMonth vencimiento = YearMonth.of(2000 + Integer.parseInt(partesVencimiento[1]), Integer.parseInt(partesVencimiento[0]));
        if (vencimiento.isBefore(YearMonth.from(LocalDate.now()))) {
            throw new IllegalArgumentException("La tarjeta está vencida.");
        }

        switch (dto.getNumeroTarjeta()) {
            case TARJETA_FONDOS_INSUFICIENTES -> throw new PagoRechazadoException(
                    "Su tarjeta fue rechazada por fondos insuficientes. Verifique su saldo e intente nuevamente.");
            case TARJETA_ERROR_COMUNICACION -> throw new PagoRechazadoException(
                    "Error al procesar el pago. Por favor, intente nuevamente o contacte a su banco.");
        }
        
        EstadoCita estadoPagada = estadoCitaRepository.findAll().stream()
                .filter(e -> "Pagada".equalsIgnoreCase(e.getNombre()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Estado 'Pagada' no configurado."));

        cita.setEstado(estadoPagada);
        cita.setReservadaHasta(null);
        citaRepository.save(cita);

        Pago pago = new Pago();
        pago.setCita(cita);
        pago.setNumeroTransaccion(UUID.randomUUID().toString());
        pago.setMonto(cita.getEspecialidad().getPrecio());
        pago.setUltimosCuatroDigitos(dto.getNumeroTarjeta().substring(dto.getNumeroTarjeta().length() - 4));
        pago.setNombreTitular(dto.getNombreTitular().toUpperCase());
        pago.setFechaPago(LocalDateTime.now());
        pagoRepository.save(pago);

        emailService.enviarCorreo(
                cita.getPaciente().getCorreo(),
                "Comprobante de pago - Cita médica",
                "Su pago fue procesado exitosamente. Número de transacción: " + pago.getNumeroTransaccion()
        );

        PagoResponseDTO response = new PagoResponseDTO();
        response.setNumeroTransaccion(pago.getNumeroTransaccion());
        response.setMedicoNombre(cita.getMedico().getNombreCompleto());
        response.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        response.setSucursalNombre(cita.getSucursal().getNombre());
        response.setFechaHoraCita(cita.getFechaHora());
        response.setMonto(pago.getMonto());
        response.setMensaje("¡Pago realizado exitosamente! Su cita ha sido confirmada.");
        return response;
    }

    private boolean esLuhnValido(String numero) {
        int suma = 0;
        boolean alternar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(numero.charAt(i));
            if (alternar) {
                digito *= 2;
                if (digito > 9) digito -= 9;
            }
            suma += digito;
            alternar = !alternar;
        }
        return suma % 10 == 0;
    }
}