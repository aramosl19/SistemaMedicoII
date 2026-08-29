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
    @Autowired private PasswordEncoder passwordEncoder;
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
                        .requestMatchers("/", "/*.html", "/favicon.ico", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/portal/registro", "/api/portal/verificar-dpi", "/api/portal/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/**").hasAnyRole("ADMINISTRADOR", "RECEPCIONISTA")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/roles/**").hasRole("ADMINISTRADOR")

                        // ---- Bitácora de Operación ----
                        .requestMatchers("/api/bitacora/**").hasRole("ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/sucursales/**", "/api/especialidades/**",
                                "/api/sucursal-especialidad/**", "/api/tipos-cita/**", "/api/estados-cita/**").authenticated()
                        .requestMatchers("/api/sucursales/**", "/api/especialidades/**",
                                "/api/sucursal-especialidad/**", "/api/tipos-cita/**", "/api/estados-cita/**").hasRole("ADMINISTRADOR")

                        .requestMatchers("/api/citas/**").hasAnyRole("PACIENTE", "RECEPCIONISTA", "MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/recepcion/**").hasAnyRole("RECEPCIONISTA", "ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/caja/citas/buscar").hasAnyRole("PACIENTE", "CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/caja/laboratorio/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/caja/**").hasAnyRole("CAJERO", "ADMINISTRADOR")
                        .requestMatchers("/api/pagos/**").hasAnyRole("PACIENTE", "CAJERO", "ADMINISTRADOR")

                        .requestMatchers("/api/medico/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/agenda/**").hasAnyRole("MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/enfermeria/**").hasAnyRole("ENFERMERO", "ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/laboratorios/**", "/api/examenes-laboratorio/**")
                        .hasAnyRole("LABORATORISTA", "SUPERVISORLABORATORIO", "MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/laboratorios/**", "/api/examenes-laboratorio/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/laboratorio/**").hasAnyRole("LABORATORISTA", "SUPERVISORLABORATORIO", "MEDICO", "ADMINISTRADOR")

                        .requestMatchers(HttpMethod.GET, "/api/medicamentos/**").hasAnyRole("FARMACEUTICO", "MEDICO", "ADMINISTRADOR")
                        .requestMatchers("/api/farmacia/**", "/api/medicamentos/**", "/api/inventario/**").hasAnyRole("FARMACEUTICO", "ADMINISTRADOR")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}