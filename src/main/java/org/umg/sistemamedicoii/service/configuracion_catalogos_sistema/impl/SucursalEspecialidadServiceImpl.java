package org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadRequestDTO;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadResponseDTO;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Auditoria;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.SucursalEspecialidad;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalEspecialidadRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.SucursalEspecialidadService;

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
    @Cacheable(value = "catalogos", key = "'sucursal_especialidad_lista'") // <-- ¡Agregado!
    public List<SucursalEspecialidadResponseDTO> listar() {
        return sucursalEspecialidadRepository.findByActivoTrue().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "catalogos", key = "'sucursal_especialidad_lista'") // <-- ¡Agregado! Borra caché al asignar
    public SucursalEspecialidadResponseDTO asignar(SucursalEspecialidadRequestDTO dto) {
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

        registrarAuditoria("ASIGNACION_CREADA", guardada.getId(),
                "Especialidad " + especialidad.getNombre() + " asignada a Sede " + sucursal.getNombre());

        return toResponseDTO(guardada);
    }

    @Override
    @CacheEvict(value = "catalogos", key = "'sucursal_especialidad_lista'") // <-- ¡Agregado! Borra caché al eliminar
    public void eliminar(Integer id) {
        SucursalEspecialidad asignacion = sucursalEspecialidadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignación no encontrada."));

        asignacion.setActivo(false);
        sucursalEspecialidadRepository.save(asignacion);

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