package org.umg.sistemamedicoii.service.facturacion_caja_pagos.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.config.cache.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.IdempotencyKey;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.PagoTarjeta;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.facturacion_caja_pagos.IdempotencyKeyRepository;
import org.umg.sistemamedicoii.repository.facturacion_caja_pagos.PagoTarjetaRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.PagoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class PagoServiceImpl implements PagoService {

    @Value("${app.hospital.nombre}")
    private String nombreHospital;

    private static final String TARJETA_RECHAZO_BANCARIO = "4000000000000002";
    private static final String TARJETA_ERROR_PROCESAMIENTO = "4000000000000101";
    private static final String TARJETA_ERROR_COMUNICACION = "4000000000000309";
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
                    "El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. Por favor, seleccione un nuevo horario. Será redirigido en unos segundos...");
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
            case TARJETA_RECHAZO_BANCARIO -> throw new PagoRechazadoException(
                    "La transacción con tarjeta fue rechazada por el banco. Por favor, verifique los datos de su tarjeta o intente con una tarjeta diferente.");
            case TARJETA_ERROR_PROCESAMIENTO -> throw new PagoRechazadoException(
                    "El pago no pudo ser procesado. Por favor, intente nuevamente o utilice otra tarjeta.");
            case TARJETA_ERROR_COMUNICACION -> throw new PagoRechazadoException(
                    "Error de comunicación con la pasarela de pago. Intente nuevamente en unos minutos.");
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

        // RN-CU04-05: el comprobante debe incluir número de transacción, monto,
        // fecha/hora de la transacción y el detalle de la cita.
        String mensajeComprobante = String.format(
                "Su pago fue procesado exitosamente.%n%n" +
                        "Número de transacción: %s%n" +
                        "Número de cita: %d%n" +
                        "Monto pagado: Q%s%n" +
                        "Fecha y hora de la transacción: %s%n%n" +
                        "Detalle de la cita:%n" +
                        "Médico: %s%n" +
                        "Especialidad: %s%n" +
                        "Sucursal: %s%n" +
                        "Fecha y hora de la cita: %s",
                pago.getNumeroTransaccion(),
                cita.getId(),
                pago.getMonto(),
                pago.getFechaPago().format(FORMATO_FECHA_HORA),
                cita.getMedico().getNombreCompleto(),
                cita.getEspecialidad().getNombre(),
                cita.getSucursal().getNombre(),
                cita.getFechaHora().format(FORMATO_FECHA_HORA)
        );

        emailService.enviarCorreo(
                cita.getPaciente().getCorreo(),
                "Comprobante de Pago - Cita Médica - " + nombreHospital,
                mensajeComprobante
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