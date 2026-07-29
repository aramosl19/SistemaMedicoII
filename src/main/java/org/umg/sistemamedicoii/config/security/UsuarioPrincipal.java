package org.umg.sistemamedicoii.config.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.umg.sistemamedicoii.models.Usuario;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class UsuarioPrincipal implements UserDetails {

    private final Usuario usuario;

    public UsuarioPrincipal(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    // Convierte "Médico" -> "MEDICO", "Farmacéutico" -> "FARMACEUTICO", etc.
    public static String normalizarRol(String nombreRol) {
        String sinAcentos = Normalizer.normalize(nombreRol, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + normalizarRol(usuario.getRol().getNombre())));
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getNombreUsuario();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        LocalDateTime bloqueadoHasta = usuario.getBloqueadoHasta();
        return bloqueadoHasta == null || !bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }
}