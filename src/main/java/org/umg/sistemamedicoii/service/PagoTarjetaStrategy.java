package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.umg.sistemamedicoii.dto.DatosCobroRequestDTO;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.models.PagoTarjeta;
import org.umg.sistemamedicoii.repository.PagoTarjetaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Component
public class PagoTarjetaStrategy implements ProcesadorPagoStrategy {

    private static final Set<String> METODOS_TARJETA = Set.of("VISA", "MASTERCARD", "DEBITO");
    private static final String TARJETA_RECHAZO_SIMULADO = "0000";

    @Autowired
    private PagoTarjetaRepository pagoTarjetaRepository;

    @Override
    public boolean soportaMetodo(String metodoPago) {
        return METODOS_TARJETA.contains(metodoPago.trim().toUpperCase());
    }

    @Override
    public BigDecimal[] procesarPago(DatosCobroRequestDTO dto, BigDecimal montoACobrar, Integer referenciaId,
                                     String nombreTitular, String numeroTransaccion, TipoConceptoCobro tipoConcepto) {
        if (dto.getUltimosCuatroDigitos() == null || !dto.getUltimosCuatroDigitos().matches("\\d{4}")) {
            throw new IllegalArgumentException("Ingrese los últimos 4 dígitos de la tarjeta.");
        }
        if (TARJETA_RECHAZO_SIMULADO.equals(dto.getUltimosCuatroDigitos())) {
            throw new PagoRechazadoException("La transacción con tarjeta fue rechazada por el banco. Solicite al paciente otro método de pago.");
        }

        PagoTarjeta pago = new PagoTarjeta();
        pago.setTipoConcepto(tipoConcepto);
        pago.setReferenciaId(referenciaId);
        pago.setNumeroTransaccion(numeroTransaccion);
        pago.setMonto(montoACobrar);
        pago.setUltimosCuatroDigitos(dto.getUltimosCuatroDigitos());
        pago.setNombreTitular(nombreTitular.toUpperCase());
        pago.setFechaPago(LocalDateTime.now());

        pagoTarjetaRepository.save(pago);

        return new BigDecimal[]{null, null};
    }
}