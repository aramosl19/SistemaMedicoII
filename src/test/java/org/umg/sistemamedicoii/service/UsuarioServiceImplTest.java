// Ubicación en tu proyecto: src/test/java/org/umg/sistemamedicoii/service/UsuarioServiceImplTest.java
package org.umg.sistemamedicoii.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.umg.sistemamedicoii.config.security.JwtService;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.exception.AccountLockedException;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.InvalidCredentialsException;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Rol;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.RolRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.UsuarioRepository;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.impl.UsuarioServiceImpl;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de UsuarioServiceImpl.
 *
 * Cubren:
 *  - CU-00 Verificación de DPI e inicio de sesión, incluyendo
 *    RN-CU00-02 (mensaje con intentos restantes) y RN-CU00-03
 *    (bloqueo de 15 minutos tras 5 intentos fallidos).
 *  - CU-02 Registro de paciente externo (correo/DPI/usuario duplicado).
 *
 * No cubre crear()/actualizar()/eliminar()/buscar() (esos pertenecen a
 * CU-01 - mantenimiento de usuarios internos) ni la generación real del
 * JWT (JwtService se mockea) — eso queda para un siguiente lote.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock private AuditoriaRepository auditoriaRepo;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;
    @Mock private SucursalRepository sucursalRepository;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtService jwtService;

    // Mockito inyecta los @Mock anteriores en los campos privados @Autowired
    // de UsuarioServiceImpl (inyección por tipo), sin necesidad de reflexión manual.
    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private Usuario paciente;
    private Rol rolPaciente;



    @BeforeEach
    void setUp() {
        rolPaciente = new Rol();
        rolPaciente.setId(1);
        rolPaciente.setNombre("Paciente");

        paciente = new Usuario();
        paciente.setId(10);
        paciente.setNombreCompleto("Ana López Pérez");
        paciente.setNombreUsuario("alopez01");
        paciente.setPassword("hash-encriptado");
        paciente.setCorreo("ana@correo.com");
        paciente.setDpi("1234567890123");
        paciente.setRol(rolPaciente);
        paciente.setActivo(true);
        paciente.setIntentosFallidos(0);
        paciente.setBloqueadoHasta(null);
    }

    private LoginRequestDTO credenciales(String usuario, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setNombreUsuario(usuario);
        dto.setPassword(password);
        return dto;
    }

    @Nested
    @DisplayName("login() - CU-00 flujo normal, RN-CU00-02 y RN-CU00-03")
    class LoginTests {

        @Test
        @DisplayName("Con credenciales válidas devuelve token y resetea intentos fallidos")
        void loginExitoso() {
            paciente.setIntentosFallidos(3);
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));
            when(passwordEncoder.matches("Clave123456", "hash-encriptado")).thenReturn(true);
            when(jwtService.generarToken(10, "alopez01", "Paciente")).thenReturn("token-jwt-123");

            LoginResponseDTO resp = usuarioService.login(credenciales("alopez01", "Clave123456"));

            assertThat(resp.getToken()).isEqualTo("token-jwt-123");
            assertThat(resp.getRol()).isEqualTo("Paciente");
            assertThat(resp.getNombreCompleto()).isEqualTo("Ana López Pérez");
            assertThat(paciente.getIntentosFallidos()).isZero();
            assertThat(paciente.getBloqueadoHasta()).isNull();
            verify(usuarioRepository).save(paciente);
        }

        @Test
        @DisplayName("Usuario inexistente lanza InvalidCredentialsException")
        void usuarioNoExiste() {
            when(usuarioRepository.findByNombreUsuario("noexiste")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.login(credenciales("noexiste", "cualquiera")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Usuario o contraseña incorrectos.");
        }

        @Test
        @DisplayName("RN-CU00-02: contraseña incorrecta incrementa intentos y reporta los restantes")
        void passwordIncorrectaIncrementaIntentos() {
            paciente.setIntentosFallidos(2); // van 2 fallos previos -> este es el 3ro
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));
            when(passwordEncoder.matches("malaClave", "hash-encriptado")).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.login(credenciales("alopez01", "malaClave")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Contraseña incorrecta. Le quedan 2 intento(s) antes de la inactivación temporal de la cuenta.");

            assertThat(paciente.getIntentosFallidos()).isEqualTo(3);
            assertThat(paciente.getBloqueadoHasta()).isNull();
            verify(usuarioRepository).save(paciente);
        }

        @Test
        @DisplayName("RN-CU00-03: el 5to intento fallido bloquea la cuenta por 15 minutos")
        void quintoIntentoFallidoBloqueaCuenta() {
            paciente.setIntentosFallidos(4); // este será el intento número 5
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));
            when(passwordEncoder.matches("malaClave", "hash-encriptado")).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.login(credenciales("alopez01", "malaClave")))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.");

            assertThat(paciente.getIntentosFallidos()).isEqualTo(5);
            assertThat(paciente.getBloqueadoHasta()).isAfter(LocalDateTime.now().plusMinutes(14));
            verify(usuarioRepository).save(paciente);
        }

        @Test
        @DisplayName("Login con la cuenta aún bloqueada lanza AccountLockedException sin evaluar la contraseña")
        void loginConCuentaBloqueada() {
            paciente.setBloqueadoHasta(LocalDateTime.now().plusMinutes(10));
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));

            assertThatThrownBy(() -> usuarioService.login(credenciales("alopez01", "Clave123456")))
                    .isInstanceOf(AccountLockedException.class);

            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Si el bloqueo ya expiró, se resetean los intentos y se evalúa la contraseña normalmente")
        void loginConBloqueoYaExpirado() {
            paciente.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
            paciente.setIntentosFallidos(5);
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));
            when(passwordEncoder.matches("Clave123456", "hash-encriptado")).thenReturn(true);
            when(jwtService.generarToken(anyInt(), anyString(), anyString())).thenReturn("token-jwt-456");

            LoginResponseDTO resp = usuarioService.login(credenciales("alopez01", "Clave123456"));

            assertThat(resp.getToken()).isEqualTo("token-jwt-456");
            assertThat(paciente.getIntentosFallidos()).isZero();
            assertThat(paciente.getBloqueadoHasta()).isNull();
        }

        @Test
        @DisplayName("Usuario inactivo no puede iniciar sesión aunque la contraseña sea correcta")
        void loginUsuarioInactivo() {
            paciente.setActivo(false);
            when(usuarioRepository.findByNombreUsuario("alopez01")).thenReturn(Optional.of(paciente));
            when(passwordEncoder.matches("Clave123456", "hash-encriptado")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.login(credenciales("alopez01", "Clave123456")))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Esta cuenta se encuentra inactiva. Contacte al administrador.");
        }
    }

    @Nested
    @DisplayName("registrarExterno() - CU-02 registro de paciente")
    class RegistroExternoTests {

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

        @Test
        @DisplayName("Con datos válidos crea el usuario con rol Paciente y envía el correo de bienvenida")
        void registroExitoso() {
            RegistroExternoRequestDTO dto = datosValidos();
            when(usuarioRepository.existsByCorreo(dto.getCorreo())).thenReturn(false);
            when(usuarioRepository.findByDpi(dto.getDpi())).thenReturn(Optional.empty());
            when(usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())).thenReturn(false);
            when(rolRepository.findByNombre("Paciente")).thenReturn(Optional.of(rolPaciente));
            when(passwordEncoder.encode(dto.getPassword())).thenReturn("hash-nueva-clave");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            UsuarioResponseDTO resultado = usuarioService.registrarExterno(dto);

            assertThat(resultado.getNombreUsuario()).isEqualTo("cramirez");
            assertThat(resultado.getRolNombre()).isEqualTo("Paciente");
            verify(emailService).enviarBienvenida(dto.getCorreo(), dto.getNombreCompleto());
        }

        @Test
        @DisplayName("Rechaza el registro si el correo ya está en uso")
        void correoDuplicado() {
            RegistroExternoRequestDTO dto = datosValidos();
            when(usuarioRepository.existsByCorreo(dto.getCorreo())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrarExterno(dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Ya existe una cuenta registrada con este correo electrónico.");

            verify(usuarioRepository, never()).save(any());
            verify(emailService, never()).enviarBienvenida(anyString(), anyString());
        }

        @Test
        @DisplayName("Rechaza el registro si el DPI ya tiene una cuenta asociada")
        void dpiDuplicado() {
            RegistroExternoRequestDTO dto = datosValidos();
            when(usuarioRepository.existsByCorreo(dto.getCorreo())).thenReturn(false);
            when(usuarioRepository.findByDpi(dto.getDpi())).thenReturn(Optional.of(paciente));

            assertThatThrownBy(() -> usuarioService.registrarExterno(dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Ya existe una cuenta registrada con este número de DPI");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza el registro si el nombre de usuario ya está tomado")
        void nombreUsuarioDuplicado() {
            RegistroExternoRequestDTO dto = datosValidos();
            when(usuarioRepository.existsByCorreo(dto.getCorreo())).thenReturn(false);
            when(usuarioRepository.findByDpi(dto.getDpi())).thenReturn(Optional.empty());
            when(usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrarExterno(dto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("El nombre de usuario ya se encuentra registrado.");
        }
    }

    @Nested
    @DisplayName("verificarDpi() - CU-00 verificación previa al agendamiento")
    class VerificarDpiTests {

        @Test
        @DisplayName("DPI registrado como paciente devuelve registrado=true con nombre y rol")
        void dpiRegistradoPaciente() {
            when(usuarioRepository.findByDpi("1234567890123")).thenReturn(Optional.of(paciente));

            VerificarDpiResponseDTO resp = usuarioService.verificarDpi("1234567890123");

            assertThat(resp.isRegistrado()).isTrue();
            assertThat(resp.getRol()).isEqualTo("Paciente");
            assertThat(resp.getNombreCompleto()).isEqualTo("Ana López Pérez");
        }

        @Test
        @DisplayName("DPI no encontrado en el sistema devuelve registrado=false")
        void dpiNoRegistrado() {
            when(usuarioRepository.findByDpi("0000000000000")).thenReturn(Optional.empty());

            VerificarDpiResponseDTO resp = usuarioService.verificarDpi("0000000000000");

            assertThat(resp.isRegistrado()).isFalse();
        }
    }
}
