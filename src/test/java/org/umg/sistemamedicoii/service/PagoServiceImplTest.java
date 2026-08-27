// Ubicación en tu proyecto: src/test/java/org/umg/sistemamedicoii/service/PagoServiceImplTest.java
package org.umg.sistemamedicoii.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.umg.sistemamedicoii.config.cache.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoRequestDTO;
import org.umg.sistemamedicoii.dto.facturacion_caja_pagos.PagoResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.enums.TipoConceptoCobro;
import org.umg.sistemamedicoii.exception.PagoRechazadoException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.IdempotencyKey;
import org.umg.sistemamedicoii.models.facturacion_caja_pagos.PagoTarjeta;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.repository.facturacion_caja_pagos.IdempotencyKeyRepository;
import org.umg.sistemamedicoii.repository.facturacion_caja_pagos.PagoTarjetaRepository;
import org.umg.sistemamedicoii.service.facturacion_caja_pagos.impl.PagoServiceImpl;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de PagoServiceImpl — CU-04 "Pago en línea de la cita médica".
 *
 * IMPORTANTE (léelo antes de correr esto): estas pruebas están escritas
 * CONTRA EL DOCUMENTO del caso de uso (4_CU_Pago_en_Linea.docx), no contra
 * lo que PagoServiceImpl ya hace hoy. Donde el código real se desvía del
 * texto/las reglas del documento, la prueba queda tal cual el documento lo
 * exige (y por lo tanto FALLARÁ hasta que se corrija el código, o hasta que
 * el ingeniero confirme que el documento debe ajustarse). Cada uno de esos
 * casos tiene un comentario "NOTA GAP" explicando exactamente la diferencia
 * encontrada, igual que se viene documentando en UsuarioServiceImplTest /
 * los specs de Cypress existentes. No se "disfrazó" ningún mock para que la
 * prueba pase contra el comportamiento actual.
 *
 * Gaps encontrados entre el documento CU-04 y PagoServiceImpl:
 *
 *  1) Postcondición del documento: "La cita queda registrada con estado
 *     'Pagada'". El enum EstadoCitaEnum NO define ningún valor PAGADA
 *     (solo existe PENDIENTE_PAGO, CONFIRMADA, CANCELADA, ...). El código
 *     real usa EstadoCitaEnum.CONFIRMADA. Esto no es solo un tema de texto:
 *     es un estado que el documento pide y que el catálogo no tiene. Hay
 *     que confirmar con el ingeniero si "Pagada" y "Confirmada" son el
 *     mismo concepto o si falta un estado en el catálogo.
 *
 *  2) FA02 (reserva expirada) — el documento pide el mensaje completo:
 *     "El tiempo para confirmar su cita ha expirado. El horario
 *     seleccionado ha sido liberado. Por favor, seleccione un nuevo
 *     horario. Será redirigido en unos segundos..."
 *     PagoServiceImpl lanza en cambio: "El tiempo para confirmar su cita
 *     ha expirado. El horario seleccionado ha sido liberado. Por favor,
 *     seleccione un nuevo horario." (le falta la última oración). Ojo:
 *     esto es aparte del mensaje que ya muestra el FRONTEND vía su propio
 *     temporizador local (ver 05_CU04_pago_en_linea.cy.js), que trae un
 *     tercer texto distinto todavía. Son tres textos distintos para la
 *     misma situación — hay que unificarlos con el ingeniero.
 *
 *  3) FA01 "tarjeta vencida" — el documento da como ejemplo el mensaje
 *     corto "La tarjeta está vencida". El código real reutiliza el mensaje
 *     de validación del DTO: "La fecha de vencimiento debe estar en
 *     formato MM/AA y la tarjeta no debe estar vencida."
 *
 *  4) [RESUELTO] FA03 (pago rechazado por la pasarela) — el documento
 *     define TRES mensajes textuales exactos (rechazo bancario / error de
 *     procesamiento / error de comunicación). PagoServiceImpl ahora usa 3
 *     tarjetas de prueba (TARJETA_RECHAZO_BANCARIO, TARJETA_ERROR_PROCESAMIENTO,
 *     TARJETA_ERROR_COMUNICACION), una por cada mensaje exacto del
 *     documento. Las pruebas de este bloque ya NO son un gap.
 *
 * Del punto 1 (estado "Pagada" vs "Confirmada") el usuario decidió, bajo su
 * propia responsabilidad y sin consultarlo con Edy, dejar el código tal
 * cual está (EstadoCitaEnum.CONFIRMADA). Se deja la prueba reflejando ese
 * comportamiento real, ya sin marcarlo como gap pendiente.
 *
 * El punto 2 (FA02) y el punto 3 (FA01 tarjeta vencida) ya se corrigieron
 * en PagoServiceImpl para que el mensaje coincida exactamente con el
 * documento — las pruebas de esos bloques también dejan de ser un gap.
 */
@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock private CitaRepository citaRepository;
    @Mock private PagoTarjetaRepository pagoTarjetaRepository;
    @Mock private EstadoCitaCache estadoCache;
    @Mock private EmailService emailService;
    @Mock private IdempotencyKeyRepository idempotencyKeyRepository;

    @InjectMocks
    private PagoServiceImpl pagoService;

    // Número de tarjeta Visa de prueba, válido bajo el algoritmo de Luhn
    // (el mismo que ya se usa en 04_CU03_agendar_citas.cy.js).
    private static final String TARJETA_VALIDA = "4111111111111111";

    private Cita cita;
    private Usuario paciente;
    private Usuario medico;
    private Sucursal sucursal;
    private Especialidad especialidad;
    private EstadoCita estadoPendientePago;
    private EstadoCita estadoConfirmada;
    private EstadoCita estadoCancelada;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pagoService, "nombreHospital", "Hospital Vida Nueva");

        paciente = new Usuario();
        paciente.setId(10);
        paciente.setNombreCompleto("Ana López Pérez");
        paciente.setCorreo("ana@correo.com");

        medico = new Usuario();
        medico.setId(500);
        medico.setNombreCompleto("Dr. Marco Solís");

        sucursal = new Sucursal();
        sucursal.setId(1);
        sucursal.setNombre("Sede Central");

        especialidad = new Especialidad();
        especialidad.setId(10);
        especialidad.setNombre("Medicina General");

        estadoPendientePago = new EstadoCita();
        estadoPendientePago.setId(1);
        estadoPendientePago.setNombre("Pendiente de pago");

        estadoConfirmada = new EstadoCita();
        estadoConfirmada.setId(2);
        estadoConfirmada.setNombre("Confirmada");

        estadoCancelada = new EstadoCita();
        estadoCancelada.setId(3);
        estadoCancelada.setNombre("Cancelada");

        cita = new Cita();
        cita.setId(900);
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setEstado(estadoPendientePago);
        cita.setFechaHora(LocalDateTime.of(2026, 8, 10, 9, 0));
        cita.setPrecio(new BigDecimal("150.00"));
        // dentro de la ventana de 5 minutos de la reserva (RN del documento, paso 1)
        cita.setReservadaHasta(LocalDateTime.now().plusMinutes(3));
    }

    private PagoRequestDTO datosPagoValidos() {
        PagoRequestDTO dto = new PagoRequestDTO();
        dto.setCitaId(900);
        dto.setNumeroTarjeta(TARJETA_VALIDA);
        dto.setNombreTitular("ana lopez");
        dto.setVencimiento("12/30");
        dto.setCvv("123");
        return dto;
    }

    // ---------------------------------------------------------------
    // Flujo normal básico (pasos 9-15 del documento)
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("procesarPago() - Flujo normal básico del documento CU-04")
    class FlujoNormalTests {

        @Test
        @DisplayName("Camino feliz: confirma el pago, arma el comprobante (RN-CU04-05) y envía el correo")
        void pagoExitoso() {
            when(idempotencyKeyRepository.findByClave("idem-key-1")).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(TipoConceptoCobro.CITA, 900))
                    .thenReturn(false);
            when(estadoCache.getEstado(EstadoCitaEnum.CONFIRMADA)).thenReturn(estadoConfirmada);
            when(pagoTarjetaRepository.save(any(PagoTarjeta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));

            PagoResponseDTO resp = pagoService.procesarPago(datosPagoValidos(), "idem-key-1");

            // RN-CU04-05: el comprobante debe traer transacción, médico,
            // especialidad, sucursal, fecha/hora de la cita y monto.
            assertThat(resp.getNumeroTransaccion()).isNotBlank();
            assertThat(resp.getMedicoNombre()).isEqualTo("Dr. Marco Solís");
            assertThat(resp.getEspecialidadNombre()).isEqualTo("Medicina General");
            assertThat(resp.getSucursalNombre()).isEqualTo("Sede Central");
            assertThat(resp.getFechaHoraCita()).isEqualTo(cita.getFechaHora());
            assertThat(resp.getMonto()).isEqualByComparingTo("150.00");

            // Postcondición del documento: "La cita queda registrada con
            // estado 'Pagada'". Ver NOTA GAP #1 en el javadoc de la clase:
            // el enum no tiene PAGADA, así que lo único verificable contra
            // el código real es que se pide el estado CONFIRMADA.
            verify(estadoCache).getEstado(EstadoCitaEnum.CONFIRMADA);
            assertThat(cita.getEstado()).isEqualTo(estadoConfirmada);
            verify(citaRepository).save(cita);

            // Postcondición del documento: "El paciente recibe un
            // comprobante de pago en su correo electrónico."
            verify(emailService).enviarCorreo(eq("ana@correo.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("El titular se guarda en mayúsculas (RN-CU04-02) y solo se persisten los últimos 4 dígitos (RN-CU04-01)")
        void guardaTitularEnMayusculasYUltimosCuatroDigitos() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);
            when(estadoCache.getEstado(EstadoCitaEnum.CONFIRMADA)).thenReturn(estadoConfirmada);
            when(pagoTarjetaRepository.save(any(PagoTarjeta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(idempotencyKeyRepository.save(any(IdempotencyKey.class))).thenAnswer(inv -> inv.getArgument(0));

            PagoRequestDTO dto = datosPagoValidos();
            dto.setNombreTitular("ana lopez");

            pagoService.procesarPago(dto, "idem-key-2");

            ArgumentCaptor<PagoTarjeta> captor = ArgumentCaptor.forClass(PagoTarjeta.class);
            verify(pagoTarjetaRepository).save(captor.capture());
            assertThat(captor.getValue().getNombreTitular()).isEqualTo("ANA LOPEZ");
            assertThat(captor.getValue().getUltimosCuatroDigitos()).isEqualTo("1111");
        }

        @Test
        @DisplayName("RNF-016: reintento con la misma Idempotency-Key devuelve la misma respuesta sin volver a cobrar")
        void idempotencia_mismaClaveNoVuelveAProcesar() {
            IdempotencyKey registroExistente = new IdempotencyKey();
            registroExistente.setClave("clave-repetida");
            registroExistente.setCitaId(900);
            registroExistente.setNumeroTransaccion("TRX-000123");
            registroExistente.setMedicoNombre("Dr. Marco Solís");
            registroExistente.setEspecialidadNombre("Medicina General");
            registroExistente.setSucursalNombre("Sede Central");
            registroExistente.setFechaHoraCita(cita.getFechaHora());
            registroExistente.setMonto(new BigDecimal("150.00"));
            registroExistente.setMensaje("¡Pago realizado exitosamente! Su cita ha sido confirmada.");

            when(idempotencyKeyRepository.findByClave("clave-repetida")).thenReturn(Optional.of(registroExistente));

            PagoResponseDTO resp = pagoService.procesarPago(datosPagoValidos(), "clave-repetida");

            assertThat(resp.getNumeroTransaccion()).isEqualTo("TRX-000123");
            verify(citaRepository, never()).findById(any());
            verify(pagoTarjetaRepository, never()).save(any());
            verify(emailService, never()).enviarCorreo(any(), any(), any());
        }

        @Test
        @DisplayName("Cita inexistente: no se puede pagar una cita que no existe [precondición del documento]")
        void citaNoEncontrada_lanzaExcepcion() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pagoService.procesarPago(datosPagoValidos(), "idem-key-3"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cita no encontrada.");
        }
    }

    // ---------------------------------------------------------------
    // FA01 - Validación de campos del formulario fallida
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("procesarPago() - FA01 Validación de campos fallida")
    class FA01Tests {

        @Test
        @DisplayName("Número de tarjeta inválido (falla Luhn) — mensaje EXACTO del documento")
        void numeroTarjetaInvalido() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);

            PagoRequestDTO dto = datosPagoValidos();
            dto.setNumeroTarjeta("4111111111111112"); // un dígito de diferencia -> Luhn inválido

            assertThatThrownBy(() -> pagoService.procesarPago(dto, "idem-key-4"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El número de tarjeta no es válido.");
        }

        @Test
        @DisplayName("Tarjeta vencida — mensaje EXACTO del documento (\"La tarjeta está vencida\")")
        void tarjetaVencida() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);

            PagoRequestDTO dto = datosPagoValidos();
            dto.setVencimiento("01/20"); // enero 2020, ya vencida

            assertThatThrownBy(() -> pagoService.procesarPago(dto, "idem-key-5"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("La tarjeta está vencida.");
        }
    }

    // ---------------------------------------------------------------
    // FA02 - Temporizador de reserva expirado
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("procesarPago() - FA02 Temporizador de reserva expirado")
    class FA02Tests {

        @Test
        @DisplayName("Reserva expirada: libera el horario (cancela la cita) y avisa con el mensaje EXACTO del documento")
        void reservaExpirada() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            cita.setReservadaHasta(LocalDateTime.now().minusSeconds(1)); // ya venció
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(estadoCache.getEstado(EstadoCitaEnum.CANCELADA)).thenReturn(estadoCancelada);

            assertThatThrownBy(() -> pagoService.procesarPago(datosPagoValidos(), "idem-key-6"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido "
                            + "liberado. Por favor, seleccione un nuevo horario. Será redirigido en unos segundos...");

            assertThat(cita.getEstado()).isEqualTo(estadoCancelada);
            verify(citaRepository).save(cita);
            // Al expirar la reserva no debe intentarse ningún cobro.
            verify(pagoTarjetaRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // FA03 - Pago rechazado por la pasarela
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("procesarPago() - FA03 Pago rechazado por la pasarela (los 3 mensajes del documento)")
    class FA03Tests {

        @Test
        @DisplayName("Rechazo bancario — mensaje EXACTO del documento")
        void rechazoBancario() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);

            PagoRequestDTO dto = datosPagoValidos();
            dto.setNumeroTarjeta("4000000000000200"); // tarjeta de prueba "fondos insuficientes"

            // NOTA GAP #4: el documento (FA03) pide exactamente:
            // "La transacción con tarjeta fue rechazada por el banco. Por
            // favor, verifique los datos de su tarjeta o intente con una
            // tarjeta diferente." El código real, para esta misma tarjeta
            // de prueba, lanza: "Su tarjeta fue rechazada por fondos
            // insuficientes. Verifique su saldo e intente nuevamente."
            // Prueba contra el documento a propósito -> VA A FALLAR.
            assertThatThrownBy(() -> pagoService.procesarPago(dto, "idem-key-7"))
                    .isInstanceOf(PagoRechazadoException.class)
                    .hasMessage("La transacción con tarjeta fue rechazada por el banco. Por favor, verifique los "
                            + "datos de su tarjeta o intente con una tarjeta diferente.");
        }

        @Test
        @DisplayName("Error de comunicación con la pasarela — mensaje EXACTO del documento")
        void errorDeComunicacion() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);

            PagoRequestDTO dto = datosPagoValidos();
            dto.setNumeroTarjeta("4000000000000309"); // tarjeta de prueba "error de comunicación"

            // NOTA GAP #4: el documento pide exactamente: "Error de
            // comunicación con la pasarela de pago. Intente nuevamente en
            // unos minutos." El código real, para esta tarjeta, lanza:
            // "Error al procesar el pago. Por favor, intente nuevamente o
            // contacte a su banco." (que en realidad suena más al texto de
            // "Error de procesamiento" del documento). Prueba contra el
            // documento a propósito -> VA A FALLAR.
            assertThatThrownBy(() -> pagoService.procesarPago(dto, "idem-key-8"))
                    .isInstanceOf(PagoRechazadoException.class)
                    .hasMessage("Error de comunicación con la pasarela de pago. Intente nuevamente en unos minutos.");
        }

        @Test
        @DisplayName("Tras un rechazo, la reserva sigue viva: el paciente puede reintentar sin perder el horario")
        void permiteReintentarSinPerderReserva() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(any(), any())).thenReturn(false);

            PagoRequestDTO dto = datosPagoValidos();
            dto.setNumeroTarjeta("4000000000000200");

            assertThatThrownBy(() -> pagoService.procesarPago(dto, "idem-key-9"))
                    .isInstanceOf(PagoRechazadoException.class);

            // El documento (FA03) exige que la reserva del horario no se
            // pierda ni se toque cuando el banco rechaza el pago.
            assertThat(cita.getReservadaHasta()).isNotNull();
            verify(citaRepository, never()).save(any());
        }
    }

    // ---------------------------------------------------------------
    // Regla de negocio adicional (no viene del documento, pero SÍ está
    // implementada): evita doble cobro de una misma cita.
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("procesarPago() - Regla adicional del código: evita cobrar dos veces la misma cita")
    class DobleCobroTests {

        @Test
        @DisplayName("Cita ya pagada: rechaza un segundo intento de cobro")
        void citaYaPagada() {
            when(idempotencyKeyRepository.findByClave(anyString())).thenReturn(Optional.empty());
            when(citaRepository.findById(900)).thenReturn(Optional.of(cita));
            when(pagoTarjetaRepository.existsByTipoConceptoAndReferenciaId(TipoConceptoCobro.CITA, 900))
                    .thenReturn(true);

            assertThatThrownBy(() -> pagoService.procesarPago(datosPagoValidos(), "idem-key-10"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Esta cita ya fue pagada.");
        }
    }
}