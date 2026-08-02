package org.umg.sistemamedicoii.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private PasswordEncoder passwordEncoder; // ya existe en SecurityBeansConfig, se reutiliza
    @Autowired private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired private RestAuthEntryPoint restAuthEntryPoint;
    @Autowired private RestAccessDeniedHandler restAccessDeniedHandler;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // ---- Archivos estáticos (Frontend) ----
                        .requestMatchers("/", "/*.html", "/favicon.ico", "/css/**", "/js/**").permitAll()

                        // ---- Publico: portal del paciente ----
                        .requestMatchers("/api/portal/registro", "/api/portal/verificar-dpi", "/api/portal/login").permitAll()

                        // ---- FIX QA: cualquier usuario autenticado puede leer SU PROPIO perfil
                        // (antes GET /api/usuarios/** era solo ADMIN/RECEPCIONISTA, y varias
                        // pantallas -farmacia_despacho.html, farmacia_inventario.html- necesitan
                        // consultar su propia sede llamando a /api/usuarios/{miId}). Debe ir
                        // ANTES de la regla general de /api/usuarios/** para que gane esta.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()

                        // ---- CU-01: administracion de usuarios y roles ----
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/roles/**").hasRole("ADMINISTRADOR")

                        // ---- Catalogos: lectura para cualquier autenticado, escritura solo Admin ----
                        .requestMatchers(HttpMethod.GET, "/api/sucursales/**", "/api/especialidades/**",
                                "/api/sucursal-especialidad/**", "/api/tipos-cita/**", "/api/estados-cita/**").authenticated()
                        .requestMatchers("/api/sucursales/**", "/api/especialidades/**",
                                "/api/sucursal-especialidad/**", "/api/tipos-cita/**", "/api/estados-cita/**").hasRole("ADMINISTRADOR")

                        // ---- CU-03 citas / CU-05 recepcion ----
                        // FIX QA: faltaba MEDICO. Sin este rol, medico_agenda.html (calendario)
                        // y el flujo de "agendar cita de seguimiento" en medico_panel.html
                        // recibian 403 al llamar /api/citas/medico/{id} y
                        // /api/citas/horarios-disponibles respectivamente.
                        .requestMatchers("/api/citas/**").hasAnyRole("PACIENTE", "RECEPCIONISTA", "MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/recepcion/**").hasAnyRole("RECEPCIONISTA", "ADMINISTRADOR")

                        // ---- CU-04/CU-06 caja y pagos (rutas especificas ANTES que las generales) ----
                        .requestMatchers(HttpMethod.GET, "/api/caja/citas/buscar").hasAnyRole("PACIENTE", "CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/caja/laboratorio/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/caja/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/pagos/**").hasAnyRole("PACIENTE", "CAJERO", "ADMINISTRADOR")

                        // ---- Consulta medica, ordenes de laboratorio y agenda del medico ----
                        .requestMatchers("/api/medico/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/agenda/**").hasAnyRole("MEDICO", "ADMINISTRADOR")

                        // ---- Enfermeria (signos vitales) ----
                        // RECEPCIONISTA incluido porque el botón "Signos Vitales (Urgente)" de recepción
                        // (FA08 de CU-05, cita con prioridad de emergencia) navega a este flujo directamente.
                        .requestMatchers("/api/enfermeria/**").hasAnyRole("ENFERMERO", "MEDICO", "RECEPCIONISTA", "ADMINISTRADOR")

                        // ---- Laboratorio ----
                        .requestMatchers("/api/laboratorios/**", "/api/laboratorio/**", "/api/examenes-laboratorio/**")
                        .hasAnyRole("LABORATORISTA", "SUPERVISORLABORATORIO", "MEDICO", "ADMINISTRADOR")

                        // ---- Farmacia / inventario ----
                        .requestMatchers(HttpMethod.GET, "/api/medicamentos/**").hasAnyRole("FARMACEUTICO", "MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/farmacia/**", "/api/medicamentos/**",
                                "/api/inventario/**").hasAnyRole("FARMACEUTICO", "ADMINISTRADOR")

                        // Cualquier otra ruta no listada: requiere estar logueado
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}