package org.umg.sistemamedicoii.controller.configuracion_catalogos_sistema;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadRequestDTO;
import org.umg.sistemamedicoii.dto.configuracion_catalogos_sistema.SucursalEspecialidadResponseDTO;
import org.umg.sistemamedicoii.service.configuracion_catalogos_sistema.SucursalEspecialidadService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sucursal-especialidad")
public class SucursalEspecialidadController {

    @Autowired
    private SucursalEspecialidadService sucursalEspecialidadService;

    @GetMapping
    public List<SucursalEspecialidadResponseDTO> listar() {
        return sucursalEspecialidadService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> asignar(@Valid @RequestBody SucursalEspecialidadRequestDTO dto) {
        SucursalEspecialidadResponseDTO asignada = sucursalEspecialidadService.asignar(dto);
        // Retornamos el formato exacto que pide el Frontend en el Flujo Normal Básico paso 8
        return Map.of(
                "mensaje", "Especialidad asignada a la sede correctamente",
                "datos", asignada
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(@PathVariable Integer id) {
        sucursalEspecialidadService.eliminar(id);
        return Map.of("mensaje", "Asignación eliminada correctamente.");
    }
}