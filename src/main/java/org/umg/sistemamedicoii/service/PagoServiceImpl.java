package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.PagoResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.IdempotencyKey;
import org.umg.sistemamedicoii.models.PagoTarjeta;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.IdempotencyKeyRepository;
import org.umg.sistemamedicoii.repository.PagoTarjetaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagoServiceImpl implements PagoService {

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    private static final String TARJETA_FONDOS_INSUFICIENTES = "4000000000000200";
    private static final String TARJETA_ERROR_COMUNICACION = "4000000000000309";

    @Autowired private CitaRepository citaRepository;
    @Autowired private PagoTarjetaRepository pagoTarjetaRepository;
    @Autowired private EstadoCitaCache estadoCache;
    @Autowired private EmailService emailService;
    @Autowired private IdempotencyKeyRepository idempotencyKeyRepository;

    @Override
    public PagoResponseDTO procesarPago(PagoRequestDTO dto, String idempotencyKey) {

        // RNF-016: si esta clave ya se procesó antes (ej. reintento de red del
        // cliente), devolvemos la misma respuesta guardada sin volver a cobrar.
        Optional<IdempotencyKey> existente = idempotencyKeyRepository.findByClave(idempotencyKey);
        if (existente.isPresent()) {
            return mapearDesdeIdempotencyKey(existente.get());
        }

        Cita cita = citaRepository.findById(dto.getCitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        if (cita.getReservadaHasta() != null && cita.getReservadaHasta().isBefore(LocalDateTime.now())) {
            cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.CANCELADA));
            citaRepository.save(cita);
            throw new IllegalArgumentException(
                    "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. Por favor, seleccione un nuevo horario.");
        }

        if (pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(TipoConceptoCobro.CITA, cita.getId())) {
            throw new IllegalArgumentException("Esta cita ya fue pagada.");
        }

        if (!esLuhnValido(dto.getNumeroTarjeta())) {
            throw new IllegalArgumentException("El número de tarjeta no es válido.");
        }

        String[] partesVencimiento = dto.getVencimiento().split("/");
        YearMonth vencimiento = YearMonth.of(2000 + Integer.parseInt(partesVencimiento[1]), Integer.parseInt(partesVencimiento[0]));
        if (vencimiento.isBefore(YearMonth.from(LocalDate.now()))) {
            throw new IllegalArgumentException("La fecha de vencimiento debe estar en formato MM/AA y la tarjeta no debe estar vencida.");
        }

        switch (dto.getNumeroTarjeta()) {
            case TARJETA_FONDOS_INSUFICIENTES -> throw new PagoRechazadoException(
                    "Su tarjeta fue rechazada por fondos insuficientes. Verifique su saldo e intente nuevamente.");
            case TARJETA_ERROR_COMUNICACION -> throw new PagoRechazadoException(
                    "Error al procesar el pago. Por favor, intente nuevamente o contacte a su banco.");
        }

        cita.setEstado(estadoCache.getEstado(EstadoCitaEnum.CONFIRMADA));
        cita.setReservadaHasta(null);
        citaRepository.save(cita);

        PagoTarjeta pago = new PagoTarjeta();
        pago.setTipoConcepto(TipoConceptoCobro.CITA);
        pago.setReferenciaId(cita.getId());
        pago.setNumeroTransaccion(UUID.randomUUID().toString());
        pago.setMonto(cita.getPrecio());
        pago.setUltimosCuatroDigitos(dto.getNumeroTarjeta().substring(dto.getNumeroTarjeta().length() - 4));
        pago.setNombreTitular(dto.getNombreTitular().toUpperCase());
        pago.setFechaPago(LocalDateTime.now());
        pagoTarjetaRepository.save(pago);

        emailService.enviarCorreo(
                cita.getPaciente().getCorreo(),
                "Comprobante de pago - Cita médica - " + nombreHospital,
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

        // RNF-016: guardamos la clave junto con la respuesta, para poder
        // reproducirla si el cliente reintenta con la misma clave.
        IdempotencyKey registro = new IdempotencyKey();
        registro.setClave(idempotencyKey);
        registro.setCitaId(cita.getId());
        registro.setNumeroTransaccion(response.getNumeroTransaccion());
        registro.setMedicoNombre(response.getMedicoNombre());
        registro.setEspecialidadNombre(response.getEspecialidadNombre());
        registro.setSucursalNombre(response.getSucursalNombre());
        registro.setFechaHoraCita(response.getFechaHoraCita());
        registro.setMonto(response.getMonto());
        registro.setMensaje(response.getMensaje());
        idempotencyKeyRepository.save(registro);

        return response;
    }

    private PagoResponseDTO mapearDesdeIdempotencyKey(IdempotencyKey registro) {
        PagoResponseDTO response = new PagoResponseDTO();
        response.setNumeroTransaccion(registro.getNumeroTransaccion());
        response.setMedicoNombre(registro.getMedicoNombre());
        response.setEspecialidadNombre(registro.getEspecialidadNombre());
        response.setSucursalNombre(registro.getSucursalNombre());
        response.setFechaHoraCita(registro.getFechaHoraCita());
        response.setMonto(registro.getMonto());
        response.setMensaje(registro.getMensaje());
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