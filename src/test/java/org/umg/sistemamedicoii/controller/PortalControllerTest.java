// Ubicación en tu proyecto: src/test/java/org/umg/sistemamedicoii/controller/PortalControllerTest.java
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
import org.umg.sistemamedicoii.dto.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.VerificarDpiRequestDTO;
import org.umg.sistemamedicoii.dto.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.exception.AccountLockedException;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.InvalidCredentialsException;
import org.umg.sistemamedicoii.service.UsuarioService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de la capa web (slice test) de /api/portal/**.
 *
 * Se deshabilitan los filtros de seguridad con addFilters=false porque estos
 * tres endpoints son públicos según SecurityConfig (permitAll); aquí solo se
 * valida el contrato HTTP del controlador (status codes, validación de
 * @Valid, mapeo de excepciones vía GlobalExceptionHandler), no la cadena de
 * autenticación JWT en sí.
 *
 * NOTA CU-02 (Reglas_de_Negocio_Consolidadas.docx): los tests de
 * validación de /api/portal/registro que agregué abajo SÍ disparan el
 * @Valid real sobre RegistroExternoRequestDTO (no mockean el mensaje), así
 * que son los que realmente comprueban que el texto exacto de cada RN-CU02
 * (incluyendo los conteos dinámicos "Usted ingresó [X] caracteres/dígitos")
 * sale tal cual del backend. El E2E de Cypress no puede comprobar esto
 * porque registro.html bloquea el envío en el navegador (HTML5
 * required/pattern/minlength) antes de llegar a hacer el fetch.
 *
 * NOTA: proyecto en Spring Boot 4 — @WebMvcTest y @AutoConfigureMockMvc viven en
 * org.springframework.boot.webmvc.test.autoconfigure (ya no en
 * org.springframework.boot.test.autoconfigure.web.servlet como en Boot 3).
 * @MockitoBean sigue siendo de Spring Framework (org.springframework.test.context.bean.override.mockito),
 * eso no cambió.
 */
@WebMvcTest(PortalController.class)
@AutoConfigureMockMvc(addFilters = false)
class PortalControllerTest {

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService userDetailsService;

    // ---------- /api/portal/login ----------

    @Test
    void login_credencialesValidas_devuelve200ConToken() throws Exception {
        LoginResponseDTO respuesta = new LoginResponseDTO();
        respuesta.setId(1);
        respuesta.setNombreCompleto("Ana López");
        respuesta.setNombreUsuario("alopez01");
        respuesta.setRol("Paciente");
        respuesta.setToken("token-jwt");
        when(usuarioService.login(any())).thenReturn(respuesta);

        LoginRequestDTO body = new LoginRequestDTO();
        body.setNombreUsuario("alopez01");
        body.setPassword("Clave123456");

        mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt"))
                .andExpect(jsonPath("$.rol").value("Paciente"));
    }

    @Test
    void login_camposVacios_devuelve400PorValidacion() throws Exception {
        LoginRequestDTO body = new LoginRequestDTO();
        body.setNombreUsuario("");
        body.setPassword("");

        mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_credencialesInvalidas_devuelve401() throws Exception {
        when(usuarioService.login(any()))
                .thenThrow(new InvalidCredentialsException("Usuario o contraseña incorrectos."));

        LoginRequestDTO body = new LoginRequestDTO();
        body.setNombreUsuario("alopez01");
        body.setPassword("incorrecta");

        mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Usuario o contraseña incorrectos."));
    }

    @Test
    void login_cuentaBloqueada_devuelve423Locked() throws Exception {
        when(usuarioService.login(any()))
                .thenThrow(new AccountLockedException("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos."));

        LoginRequestDTO body = new LoginRequestDTO();
        body.setNombreUsuario("alopez01");
        body.setPassword("Clave123456");

        mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isLocked());
    }

    // ---------- /api/portal/verificar-dpi ----------

    @Test
    void verificarDpi_formatoInvalido_devuelve400() throws Exception {
        VerificarDpiRequestDTO body = new VerificarDpiRequestDTO();
        body.setDpi("123"); // menos de 13 dígitos

        mockMvc.perform(post("/api/portal/verificar-dpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verificarDpi_registrado_devuelve200ConDatosDelPaciente() throws Exception {
        VerificarDpiResponseDTO respuesta = new VerificarDpiResponseDTO();
        respuesta.setRegistrado(true);
        respuesta.setRol("Paciente");
        respuesta.setNombreCompleto("Ana López");
        when(usuarioService.verificarDpi("1234567890123")).thenReturn(respuesta);

        VerificarDpiRequestDTO body = new VerificarDpiRequestDTO();
        body.setDpi("1234567890123");

        mockMvc.perform(post("/api/portal/verificar-dpi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrado").value(true))
                .andExpect(jsonPath("$.rol").value("Paciente"));
    }

    // ---------- /api/portal/registro ----------

    @Test
    void registro_correoDuplicado_devuelve409Conflict() throws Exception {
        when(usuarioService.registrarExterno(any()))
                .thenThrow(new DuplicateResourceException("Ya existe una cuenta registrada con este correo electrónico."));

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosValidos())))
                .andExpect(status().isConflict());
    }

    @Test
    void registro_datosValidos_devuelve201Created() throws Exception {
        UsuarioResponseDTO respuesta = new UsuarioResponseDTO();
        respuesta.setId(5);
        respuesta.setNombreUsuario("cramirez");
        respuesta.setRolNombre("Paciente");
        when(usuarioService.registrarExterno(any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(datosValidos())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreUsuario").value("cramirez"));
    }

    // RN-CU02-03: el número de afiliado es opcional. Con un valor válido
    // dentro de rango (5-50 caracteres) el registro debe seguir aceptándose.
    @Test
    void registro_conNumeroSeguroValido_devuelve201Created() throws Exception {
        UsuarioResponseDTO respuesta = new UsuarioResponseDTO();
        respuesta.setId(5);
        respuesta.setNombreUsuario("cramirez");
        respuesta.setRolNombre("Paciente");
        when(usuarioService.registrarExterno(any())).thenReturn(respuesta);

        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNumeroSeguro("AFIL-004521");

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    // RN-CU02-03: si SÍ se ingresa número de seguro, debe respetar 5-50 caracteres.
    @Test
    void registro_numeroSeguroFueraDeRango_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNumeroSeguro("ab1"); // 3 caracteres, menos del mínimo de 5

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.numeroSeguro")
                        .value("El número de seguro debe contener entre 5 y 50 caracteres."));
    }

    // RN-CU02-01: nombre completo, 10-100 caracteres, con conteo dinámico.
    @Test
    void registro_nombreDemasiadoCorto_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNombreCompleto("Carlos"); // 6 caracteres

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombreCompleto")
                        .value("El nombre debe contener entre 10 y 100 caracteres. Usted ingresó 6 caracteres."));
    }

    // RN-GLOBAL-001: DPI únicamente numérico (13 caracteres, pero con letras).
    @Test
    void registro_dpiConLetras_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setDpi("12345678901ab"); // 13 caracteres, pero no todos dígitos

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dpi")
                        .value("El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales."));
    }

    // RN-GLOBAL-001: DPI con longitud incorrecta (numérico, pero no 13 dígitos), con conteo dinámico.
    @Test
    void registro_dpiConLongitudIncorrecta_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setDpi("123456"); // 6 dígitos

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dpi")
                        .value("El DPI debe contener exactamente 13 dígitos. Usted ingresó 6 dígitos."));
    }

    // RN-GLOBAL-002: NIT, 8-9 caracteres, con conteo dinámico.
    @Test
    void registro_nitDemasiadoCorto_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNit("1234567"); // 7 caracteres

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nit")
                        .value("El NIT debe contener entre 8 y 9 caracteres. Usted ingresó 7 caracteres."));
    }

    // RN-CU02-02: teléfono, exactamente 8 dígitos.
    @Test
    void registro_telefonoConMenosDe8Digitos_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setTelefono("5551234"); // 7 dígitos

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.telefono")
                        .value("El número de teléfono debe contener exactamente 8 dígitos numéricos."));
    }

    // RN-CU02-04: correo, formato válido.
    @Test
    void registro_correoConFormatoInvalido_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setCorreo("correo-invalido");

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo")
                        .value("El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com"));
    }

    // RN-CU02-05: usuario, mensaje DISTINTO cuando es muy corto (< 8).
    @Test
    void registro_usuarioDemasiadoCorto_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNombreUsuario("abc"); // 3 caracteres

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombreUsuario")
                        .value("El usuario debe contener al menos 8 caracteres."));
    }

    // RN-CU02-05: usuario, mensaje DISTINTO cuando es muy largo (> 9).
    @Test
    void registro_usuarioDemasiadoLargo_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setNombreUsuario("cramirezlopez"); // 13 caracteres

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombreUsuario")
                        .value("El usuario no puede exceder los 9 caracteres."));
    }

    // RN-CU02-06: contraseña, mínimo 12 caracteres.
    @Test
    void registro_passwordDemasiadoCorto_devuelve400ConMensajeExacto() throws Exception {
        RegistroExternoRequestDTO dto = datosValidos();
        dto.setPassword("corta1");

        mockMvc.perform(post("/api/portal/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password")
                        .value("La contraseña debe contener al menos 12 caracteres."));
    }

    private RegistroExternoRequestDTO datosValidos() {
        RegistroExternoRequestDTO dto = new RegistroExternoRequestDTO();
        dto.setNombreCompleto("Carlos Iván Ramírez");
        dto.setDpi("9876543210123");
        dto.setNit("12345678");
        dto.setTelefono("55512345");
        dto.setCorreo("carlos@correo.com");
        dto.setNombreUsuario("cramirez");
        dto.setPassword("ContraseñaSegura123");
        return dto;
    }
}