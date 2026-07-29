package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.SucursalEspecialidadRequestDTO;
import org.umg.sistemamedicoii.dto.SucursalEspecialidadResponseDTO;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Auditoria;
import org.umg.sistemamedicoii.models.Especialidad;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.models.SucursalEspecialidad;
import org.umg.sistemamedicoii.repository.AuditoriaRepository;
import org.umg.sistemamedicoii.repository.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.SucursalEspecialidadRepository;
import org.umg.sistemamedicoii.repository.SucursalRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SucursalEspecialidadServiceImpl implements SucursalEspecialidadService {

    @Autowired private SucursalEspecialidadRepository sucursalEspecialidadRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private AuditoriaRepository auditoriaRepository;

    @Override
    public List<SucursalEspecialidadResponseDTO> listar() {
        return sucursalEspecialidadRepository.findByActivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SucursalEspecialidadResponseDTO asignar(SucursalEspecialidadRequestDTO dto) {
        // RN-CU12-01: Verificación de duplicados (FA05)
        if (sucursalEspecialidadRepository.existsBySucursalIdAndEspecialidadIdAndActivoTrue(dto.getSucursalId(), dto.getEspecialidadId())) {
            throw new DuplicateResourceException("Esta combinación de sede y especialidad ya existe en el sistema.");
        }

        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));

        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada."));

        SucursalEspecialidad asignacion = new SucursalEspecialidad();
        asignacion.setSucursal(sucursal);
        asignacion.setEspecialidad(especialidad);
        asignacion.setActivo(true);

        SucursalEspecialidad guardada = sucursalEspecialidadRepository.save(asignacion);

        // Registro de Auditoría (Postcondiciones 2.6)
        registrarAuditoria("ASIGNACION_CREADA", guardada.getId(),
                "Especialidad " + especialidad.getNombre() + " asignada a Sede " + sucursal.getNombre());

        return toResponseDTO(guardada);
    }

    @Override
    public void eliminar(Integer id) {
        SucursalEspecialidad asignacion = sucursalEspecialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada."));

        // FA02: Eliminación Lógica
        asignacion.setActivo(false);
        sucursalEspecialidadRepository.save(asignacion);

        // Registro de Auditoría (Postcondiciones 2.6)
        registrarAuditoria("ASIGNACION_ELIMINADA", asignacion.getId(),
                "Asignación eliminada: Especialidad " + asignacion.getEspecialidad().getNombre() +
                        " removida de Sede " + asignacion.getSucursal().getNombre());
    }

    private SucursalEspecialidadResponseDTO toResponseDTO(SucursalEspecialidad entidad) {
        SucursalEspecialidadResponseDTO dto = new SucursalEspecialidadResponseDTO();
        dto.setId(entidad.getId());
        dto.setSucursalId(entidad.getSucursal().getId());
        dto.setSucursalNombre(entidad.getSucursal().getNombre());
        dto.setEspecialidadId(entidad.getEspecialidad().getId());
        dto.setEspecialidadNombre(entidad.getEspecialidad().getNombre());
        dto.setActivo(entidad.isActivo());
        return dto;
    }

    private void registrarAuditoria(String accion, Integer entidadId, String detalle) {
        Auditoria log = new Auditoria();
        log.setAccion(accion);
        log.setEntidadAfectada("SUCURSAL_ESPECIALIDAD");
        log.setEntidadId(entidadId);
        log.setDetalle(detalle);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
            log.setUsuarioEjecutorId(principal.getUsuario().getId());
        } else {
            log.setUsuarioEjecutorId(null);
        }

        log.setFechaHora(LocalDateTime.now());
        auditoriaRepository.save(log);
    }
}