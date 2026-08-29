package org.umg.sistemamedicoii.aop;

import org.springframework.security.core.context.SecurityContextHolder;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;

import java.time.LocalDateTime;

public final class AuditoriaHelper {

    private AuditoriaHelper() {}

    public static void registrar(AuditoriaRepository repo, String accion, String entidadAfectada,
                                 Integer entidadId, String detalle) {
        Auditoria log = new Auditoria();
        log.setAccion(accion);
        log.setEntidadAfectada(entidadAfectada);
        log.setEntidadId(entidadId);
        log.setDetalle(detalle);
        log.setFechaHora(LocalDateTime.now());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal principal) {
            var usuario = principal.getUsuario();
            log.setUsuarioEjecutorId(usuario.getId());
            log.setNombreUsuario(usuario.getNombreUsuario());
            log.setNombreReal(usuario.getNombreCompleto());
            log.setRol(usuario.getRol().getNombre());
        } else {
            log.setNombreUsuario("SISTEMA");
            log.setNombreReal("SISTEMA");
            log.setRol("SISTEMA");
        }

        repo.save(log);
    }
}