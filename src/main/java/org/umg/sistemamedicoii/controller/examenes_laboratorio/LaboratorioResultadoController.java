package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    // FIX QA (gap #10 de la verificación con Edy — CU-09): un médico solo debe ver
    // las órdenes/resultados de SUS PROPIOS pacientes. Laboratorista/Supervisor/
    // Administrador siguen viendo todas las órdenes (medicoId = null => sin filtro).
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

    @PostMapping("/examenes/{detalleId}/resultado")
    public DetalleOrdenLaboratorioResponseDTO registrarResultado(
            @PathVariable Integer detalleId,
            @RequestBody RegistrarResultadoRequestDTO dto) {
        return laboratorioResultadoService.registrarResultado(detalleId, dto);
    }

    // FIX QA (punto 1/12/15 confirmado por Edy): sin el rol SUPERVISORLABORATORIO
    // no se puede publicar ningún resultado — se quita LABORATORISTA de la anotación.
    @PostMapping("/examenes/{detalleId}/publicar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SUPERVISORLABORATORIO')")
    public DetalleOrdenLaboratorioResponseDTO publicarResultado(@PathVariable Integer detalleId) {
        return laboratorioResultadoService.publicarResultado(detalleId);
    }

    private boolean esMedico(UsuarioPrincipal principal) {
        String rol = UsuarioPrincipal.normalizarRol(principal.getUsuario().getRol().getNombre());
        return "MEDICO".equals(rol);
    }
}