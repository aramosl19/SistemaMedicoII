// Ubicación en tu proyecto: src/test/java/org/umg/sistemamedicoii/controller/CitaControllerTest.java
package org.umg.sistemamedicoii.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.umg.sistemamedicoii.config.security.CustomUserDetailsService;
import org.umg.sistemamedicoii.config.security.JwtService;
import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.service.AntivirusService;
import org.umg.sistemamedicoii.service.BlobStorageService;
import org.umg.sistemamedicoii.service.CitaService;
import org.umg.sistemamedicoii.service.PdfContentValidationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web (slice test) de /api/citas/**, enfocada en CU-03
 * (agendamiento de citas desde el portal).
 *
 * Se deshabilitan los filtros de seguridad con addFilters=false por la misma
 * razón que en PortalControllerTest: aquí se valida el contrato HTTP del
 * controlador, no la cadena de autenticación JWT.
 *
 * NOTA: /api/citas/mis-citas usa @AuthenticationPrincipal y no se cubre en
 * este lote — requeriría la dependencia spring-security-test para simular
 * el UsuarioPrincipal autenticado, que aún no está en el pom.xml.
 *
 * NOTA: proyecto en Spring Boot 4 — @WebMvcTest y @AutoConfigureMockMvc viven en
 * org.springframework.boot.webmvc.test.autoconfigure (ya no en
 * org.springframework.boot.test.autoconfigure.web.servlet como en Boot 3).
 */
@WebMvcTest(CitaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CitaControllerTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CustomUserDetailsService userDetailsService;
    @MockitoBean private CitaService citaService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AntivirusService antivirusService;
    @MockitoBean private PdfContentValidationService pdfContentValidationService;
    @MockitoBean private BlobStorageService blobStorageService;
    @MockitoBean private CitaRepository citaRepository;

    private CitaRequestDTO datosValidos() {
        CitaRequestDTO dto = new CitaRequestDTO();
        dto.setPacienteId(10);
        dto.setMedicoId(20);
        dto.setSucursalId(1);
        dto.setEspecialidadId(3);
        dto.setTipoCitaId(1);
        dto.setFechaHora(LocalDateTime.now().plusDays(2));
        dto.setMotivo("Dolor de cabeza persistente desde hace tres días.");
        return dto;
    }

    // ---------- GET /api/citas/medicos-disponibles ----------

    @Test
    void medicosDisponibles_devuelve200ConLista() throws Exception {
        MedicoDisponibleResponseDTO medico = new MedicoDisponibleResponseDTO();
        medico.setId(20);
        medico.setNombreCompleto("Dr. Carlos Estrada");
        when(citaService.listarMedicosDisponibles(1, 3)).thenReturn(List.of(medico));

        mockMvc.perform(get("/api/citas/medicos-disponibles")
                        .param("sucursalId", "1")
                        .param("especialidadId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCompleto").value("Dr. Carlos Estrada"));
    }

    // ---------- GET /api/citas/horarios-disponibles ----------

    @Test
    void horariosDisponibles_devuelve200ConLista() throws Exception {
        LocalDateTime slot = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        when(citaService.listarHorariosDisponibles(any(), any())).thenReturn(List.of(slot));

        mockMvc.perform(get("/api/citas/horarios-disponibles")
                        .param("medicoId", "20")
                        .param("fecha", slot.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    // ---------- POST /api/citas ----------

    @Test
    void agendar_datosValidos_devuelve201Created() throws Exception {
        CitaResponseDTO respuesta = new CitaResponseDTO();
        respuesta.setId(100);
        respuesta.setEstadoNombre("Pendiente de pago");
        respuesta.setPacienteNombre("Ana López Pérez");
        when(citaService.agendarCita(any(), anyBoolean())).thenReturn(respuesta);

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosValidos())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoNombre").value("Pendiente de pago"));
    }

    @Test
    void agendar_camposObligatoriosVacios_devuelve400PorValidacion() throws Exception {
        CitaRequestDTO body = new CitaRequestDTO();

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agendar_horarioNoDisponible_devuelve400() throws Exception {
        when(citaService.agendarCita(any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("El horario seleccionado ya no esta disponible. Por favor, elija otro horario."));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosValidos())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El horario seleccionado ya no esta disponible. Por favor, elija otro horario."));
    }

    @Test
    void agendar_pacienteInexistente_devuelve404() throws Exception {
        when(citaService.agendarCita(any(), anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Paciente no encontrado."));

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosValidos())))
                .andExpect(status().isNotFound());
    }

    @Test
    void agendar_motivoDemasiadoCorto_devuelve400() throws Exception {
        CitaRequestDTO body = datosValidos();
        body.setMotivo("corto");

        mockMvc.perform(post("/api/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
