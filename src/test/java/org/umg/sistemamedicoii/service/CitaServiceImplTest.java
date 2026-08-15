// Ubicación en tu proyecto: src/test/java/org/umg/sistemamedicoii/service/CitaServiceImplTest.java
package org.umg.sistemamedicoii.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.umg.sistemamedicoii.config.EstadoCitaCache;
import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.enums.EstadoCitaEnum;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.*;
import org.umg.sistemamedicoii.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de CitaServiceImpl.
 *
 * Cubren CU-03 (Agendar Citas):
 *  - Flujo normal de agendamiento (estado "Pendiente de pago", reserva de 5 min).
 *  - RN-CU03-05 fecha/hora debe ser futura.
 *  - Validación de horario ya ocupado y de bloqueos de agenda (EventoAgenda).
 *  - Diferencia entre agendamiento de portal (reservadaHasta con timer) y
 *    agendamiento de personal interno / walk-in [CU-05] (sin timer).
 *  - RN-CU11-01/03 validación de citas de seguimiento (tipo y prioridad).
 *  - listarMedicosDisponibles() y listarHorariosDisponibles().
 *
 * No cubre el flujo de pago (CU-04) ni la lógica del calendario médico (CU-16).
 */
@ExtendWith(MockitoExtension.class)
class CitaServiceImplTest {

    @Mock private EmailService emailService;
    @Mock private TipoCitaRepository tipoCitaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CitaRepository citaRepository;
    @Mock private SucursalRepository sucursalRepository;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private EstadoCitaCache estadoCache;
    @Mock private EventoAgendaRepository eventoAgendaRepository;

    @InjectMocks
    private CitaServiceImpl citaService;

    private Usuario paciente;
    private Usuario medico;
    private Sucursal sucursal;
    private Especialidad especialidad;
    private TipoCita tipoCita;
    private EstadoCita estadoPendientePago;
    private CitaRequestDTO dtoValido;
    private LocalDateTime fechaFutura;

    @BeforeEach
    void setUp() {
        paciente = new Usuario();
        paciente.setId(10);
        paciente.setNombreCompleto("Ana López Pérez");
        paciente.setCorreo("ana@correo.com");

        Rol rolMedico = new Rol();
        rolMedico.setId(2);
        rolMedico.setNombre("Médico");

        medico = new Usuario();
        medico.setId(20);
        medico.setNombreCompleto("Dr. Carlos Estrada");
        medico.setRol(rolMedico);
        medico.setActivo(true);

        sucursal = new Sucursal();
        sucursal.setId(1);
        sucursal.setNombre("Sede Central");
        medico.setSucursal(sucursal);

        especialidad = new Especialidad();
        especialidad.setId(3);
        especialidad.setNombre("Medicina General");
        medico.setEspecialidad(especialidad);

        tipoCita = new TipoCita();
        tipoCita.setId(1);
        tipoCita.setNombre("Consulta General");
        tipoCita.setPrecio(new BigDecimal("150.00"));

        estadoPendientePago = new EstadoCita();
        estadoPendientePago.setId(1);
        estadoPendientePago.setNombre("Pendiente de pago");

        fechaFutura = LocalDateTime.now().plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);

        dtoValido = new CitaRequestDTO();
        dtoValido.setPacienteId(10);
        dtoValido.setMedicoId(20);
        dtoValido.setSucursalId(1);
        dtoValido.setEspecialidadId(3);
        dtoValido.setTipoCitaId(1);
        dtoValido.setFechaHora(fechaFutura);
        dtoValido.setMotivo("Dolor de cabeza persistente desde hace tres días.");
    }

    private void mockearCatalogosBasicos() {
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(paciente));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(medico));
        when(sucursalRepository.findById(1)).thenReturn(Optional.of(sucursal));
        when(especialidadRepository.findById(3)).thenReturn(Optional.of(especialidad));
        when(tipoCitaRepository.findById(1)).thenReturn(Optional.of(tipoCita));
        when(estadoCache.getEstado(EstadoCitaEnum.PENDIENTE_PAGO)).thenReturn(estadoPendientePago);
        when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                anyInt(), any(), any())).thenReturn(Collections.emptyList());
    }

    @Nested
    @DisplayName("agendarCita() - CU-03 flujo normal y RN-CU03-05")
    class AgendarCitaTests {

        @Test
        @DisplayName("Con datos válidos desde el portal, agenda la cita con estado Pendiente de pago y reserva de 5 minutos")
        void agendarDesdePortalReservaHorario() {
            mockearCatalogosBasicos();
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            CitaResponseDTO resp = citaService.agendarCita(dtoValido, false);

            assertThat(resp.getEstadoNombre()).isEqualTo("Pendiente de pago");
            assertThat(resp.getPacienteNombre()).isEqualTo("Ana López Pérez");
            assertThat(resp.getMedicoNombre()).isEqualTo("Dr. Carlos Estrada");

            org.mockito.ArgumentCaptor<Cita> captor = org.mockito.ArgumentCaptor.forClass(Cita.class);
            verify(citaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservadaHasta()).isNotNull();
            assertThat(captor.getValue().getReservadaHasta()).isAfter(LocalDateTime.now().plusMinutes(4));
            assertThat(captor.getValue().isCreadaPorPersonalInterno()).isFalse();
        }

        @Test
        @DisplayName("Creada por personal interno (walk-in CU-05) no fija temporizador de reserva")
        void agendarPorPersonalInternoNoReservaHorario() {
            mockearCatalogosBasicos();
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.agendarCita(dtoValido, true);

            org.mockito.ArgumentCaptor<Cita> captor = org.mockito.ArgumentCaptor.forClass(Cita.class);
            verify(citaRepository).save(captor.capture());
            assertThat(captor.getValue().getReservadaHasta()).isNull();
            assertThat(captor.getValue().isCreadaPorPersonalInterno()).isTrue();
        }

        @Test
        @DisplayName("RN-CU03-05: fecha/hora en el pasado o presente lanza IllegalArgumentException")
        void fechaNoFuturaLanzaExcepcion() {
            dtoValido.setFechaHora(LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Debe seleccionar una fecha y hora futuras. Las citas no pueden agendarse en fechas pasadas o presentes.");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Horario ya ocupado por otra cita del mismo médico lanza IllegalArgumentException")
        void horarioYaOcupadoLanzaExcepcion() {
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(true);

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El horario seleccionado ya no esta disponible. Por favor, elija otro horario.");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Horario dentro de un bloqueo de agenda del médico lanza IllegalArgumentException")
        void horarioBloqueadoPorEventoAgendaLanzaExcepcion() {
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);

            EventoAgenda bloqueo = new EventoAgenda();
            bloqueo.setFechaInicio(fechaFutura.minusMinutes(10));
            bloqueo.setFechaFin(fechaFutura.plusMinutes(10));
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    eq(20), any(), any())).thenReturn(List.of(bloqueo));

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El horario seleccionado no está disponible porque el médico tiene un bloqueo de agenda. Por favor, elija otro horario.");

            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Paciente inexistente lanza ResourceNotFoundException")
        void pacienteInexistenteLanzaExcepcion() {
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    anyInt(), any(), any())).thenReturn(Collections.emptyList());
            when(usuarioRepository.findById(10)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Paciente no encontrado.");
        }
    }

    @Nested
    @DisplayName("agendarCita() - RN-CU11-01 y RN-CU11-03 validación de seguimiento")
    class SeguimientoTests {

        @Test
        @DisplayName("Cita de seguimiento sin tipo especificado lanza IllegalArgumentException")
        void seguimientoSinTipoLanzaExcepcion() {
            dtoValido.setCitaPadreId(99);
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    anyInt(), any(), any())).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Debe seleccionar el tipo de seguimiento.");
        }

        @Test
        @DisplayName("Tipo de seguimiento fuera de las opciones permitidas lanza IllegalArgumentException")
        void tipoSeguimientoInvalidoLanzaExcepcion() {
            dtoValido.setCitaPadreId(99);
            dtoValido.setTipoSeguimiento("Chequeo general");
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    anyInt(), any(), any())).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tipo de seguimiento inválido");
        }

        @Test
        @DisplayName("RN-CU11-03: seguimiento sin prioridad especificada lanza IllegalArgumentException")
        void seguimientoSinPrioridadLanzaExcepcion() {
            dtoValido.setCitaPadreId(99);
            dtoValido.setTipoSeguimiento("Monitoreo de Tratamiento");
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    anyInt(), any(), any())).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> citaService.agendarCita(dtoValido, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Debe seleccionar la prioridad del seguimiento.");
        }

        @Test
        @DisplayName("Seguimiento válido guarda la cita y notifica al paciente por correo")
        void seguimientoValidoEnviaCorreo() {
            dtoValido.setCitaPadreId(99);
            dtoValido.setTipoSeguimiento("Revisión de Resultados de Laboratorio");
            dtoValido.setPrioridadSeguimiento("Alta");
            mockearCatalogosBasicos();
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> {
                Cita c = inv.getArgument(0);
                c.setId(555);
                return c;
            });

            citaService.agendarCita(dtoValido, false);

            verify(emailService).enviarCorreo(eq("ana@correo.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("Cita normal (sin citaPadreId) no envía correo de seguimiento")
        void citaSinSeguimientoNoEnviaCorreo() {
            mockearCatalogosBasicos();
            when(citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                    eq(20), eq(fechaFutura), eq("Cancelada"))).thenReturn(false);
            when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

            citaService.agendarCita(dtoValido, false);

            verify(emailService, never()).enviarCorreo(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("listarMedicosDisponibles() y listarHorariosDisponibles() - CU-03 pasos 3 y 4")
    class DisponibilidadTests {

        @Test
        @DisplayName("Solo devuelve médicos activos que coinciden con sucursal y especialidad")
        void listarMedicosDisponiblesFiltraCorrectamente() {
            Rol rolEnfermero = new Rol();
            rolEnfermero.setNombre("Enfermero");
            Usuario enfermero = new Usuario();
            enfermero.setId(30);
            enfermero.setActivo(true);
            enfermero.setRol(rolEnfermero);
            enfermero.setSucursal(sucursal);
            enfermero.setEspecialidad(especialidad);

            Usuario medicoInactivo = new Usuario();
            medicoInactivo.setId(40);
            medicoInactivo.setActivo(false);
            Rol rolMedico2 = new Rol();
            rolMedico2.setNombre("Médico");
            medicoInactivo.setRol(rolMedico2);
            medicoInactivo.setSucursal(sucursal);
            medicoInactivo.setEspecialidad(especialidad);

            when(usuarioRepository.findAll()).thenReturn(List.of(medico, enfermero, medicoInactivo));

            List<MedicoDisponibleResponseDTO> resultado = citaService.listarMedicosDisponibles(1, 3);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).getId()).isEqualTo(20);
            assertThat(resultado.get(0).getNombreCompleto()).isEqualTo("Dr. Carlos Estrada");
        }

        @Test
        @DisplayName("Genera slots de 30 minutos entre 08:00 y 17:00 excluyendo horarios ya ocupados")
        void listarHorariosDisponiblesExcluyeOcupados() {
            LocalDate fecha = LocalDate.now().plusDays(1);
            LocalDateTime ocupado = fecha.atTime(9, 0);

            Cita citaExistente = new Cita();
            citaExistente.setFechaHora(ocupado);
            when(citaRepository.findByMedicoIdAndFechaHoraBetweenAndEstado_NombreNot(
                    eq(20), any(), any(), eq("Cancelada"))).thenReturn(List.of(citaExistente));
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    eq(20), any(), any())).thenReturn(Collections.emptyList());

            List<LocalDateTime> horarios = citaService.listarHorariosDisponibles(20, fecha);

            assertThat(horarios).hasSize(17); // (17:00 - 08:00) / 30min = 18 slots, menos 1 ocupado
            assertThat(horarios).doesNotContain(ocupado);
            assertThat(horarios).contains(fecha.atTime(8, 0));
        }

        @Test
        @DisplayName("Un bloqueo de agenda del médico también quita el horario de la lista de disponibles")
        void listarHorariosDisponiblesExcluyeBloqueosDeAgenda() {
            LocalDate fecha = LocalDate.now().plusDays(1);
            when(citaRepository.findByMedicoIdAndFechaHoraBetweenAndEstado_NombreNot(
                    eq(20), any(), any(), eq("Cancelada"))).thenReturn(Collections.emptyList());

            EventoAgenda bloqueo = new EventoAgenda();
            bloqueo.setFechaInicio(fecha.atTime(10, 0));
            bloqueo.setFechaFin(fecha.atTime(11, 0));
            when(eventoAgendaRepository.findByMedicoIdAndFechaInicioLessThanAndFechaFinGreaterThan(
                    eq(20), any(), any())).thenReturn(List.of(bloqueo));

            List<LocalDateTime> horarios = citaService.listarHorariosDisponibles(20, fecha);

            assertThat(horarios).doesNotContain(fecha.atTime(10, 0), fecha.atTime(10, 30));
            assertThat(horarios).contains(fecha.atTime(9, 30), fecha.atTime(11, 0));
        }
    }
}
