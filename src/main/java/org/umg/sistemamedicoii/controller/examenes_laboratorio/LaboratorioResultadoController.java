package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.RegistrarResultadoRequestDTO;
import org.umg.sistemamedicoii.service.examenes_laboratorio.LaboratorioResultadoService;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorio")
public class LaboratorioResultadoController {

    @Autowired
    private LaboratorioResultadoService laboratorioResultadoService;

    @GetMapping("/ordenes")
    public List<OrdenLaboratorioResponseDTO> listarOrdenes(
            @RequestParam(defaultValue = "EN_PROCESO") String estado,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Integer medicoId = esMedico(principal) ? principal.getUsuario().getId() : null;
        return laboratorioResultadoService.listarOrdenes(estado, medicoId);
    }

    @GetMapping("/ordenes/{id}")
    public OrdenLaboratorioResponseDTO obtenerDetalle(
            @PathVariable Integer id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        Integer medicoId = esMedico(principal) ? principal.getUsuario().getId() : null;
        return laboratorioResultadoService.obtenerDetalle(id, medicoId);
    }

    @Auditable(value = "Registró resultado de laboratorio", entidad = "ORDEN_LABORATORIO")
    @PostMapping("/examenes/{detalleId}/resultado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISORLABORATORIO', 'LABORATORISTA')")
    public DetalleOrdenLaboratorioResponseDTO registrarResultado(
            @PathVariable Integer detalleId,
            @RequestBody RegistrarResultadoRequestDTO dto) {
        return laboratorioResultadoService.registrarResultado(detalleId, dto);
    }

    @Auditable(value = "Publicó resultado de laboratorio", entidad = "ORDEN_LABORATORIO")
    @PostMapping("/examenes/{detalleId}/publicar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISORLABORATORIO')")
    public DetalleOrdenLaboratorioResponseDTO publicarResultado(@PathVariable Integer detalleId) {
        return laboratorioResultadoService.publicarResultado(detalleId);
    }

    @Auditable(value = "Reabrió resultado de laboratorio", entidad = "ORDEN_LABORATORIO")
    @PostMapping("/examenes/{detalleId}/reabrir")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISORLABORATORIO')")
    public DetalleOrdenLaboratorioResponseDTO reabrirResultado(@PathVariable Integer detalleId) {
        return laboratorioResultadoService.reabrirResultado(detalleId);
    }

    private boolean esMedico(UsuarioPrincipal principal) {
        String rol = UsuarioPrincipal.normalizarRol(principal.getUsuario().getRol().getNombre());
        return "MEDICO".equals(rol);
    }
}