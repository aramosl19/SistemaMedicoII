package org.umg.sistemamedicoii.controller.gestion_citas_recepcion;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.*;
import org.umg.sistemamedicoii.service.gestion_citas_recepcion.CitaService;
import org.umg.sistemamedicoii.service.gestion_citas_recepcion.RecepcionService;

@RestController
@RequestMapping("/api/recepcion")
public class RecepcionController {

    @Autowired
    private RecepcionService recepcionService;

    @Autowired
    private CitaService citaService;

    @GetMapping("/citas/buscar")
    public BusquedaRecepcionResponseDTO buscar(
            @RequestParam(required = false) Integer numeroCita,
            @RequestParam(required = false) String dpi) {
        return recepcionService.buscar(numeroCita, dpi);
    }

    @Auditable(value = "Registró llegada de paciente", entidad = "CITA")
    @PostMapping("/citas/{id}/llegada")
    public CitaRecepcionResponseDTO registrarLlegada(
            @PathVariable Integer id,
            @RequestBody(required = false) RegistrarLlegadaRequestDTO dto) {
        boolean esEmergencia = dto != null && dto.isEmergencia();
        return recepcionService.registrarLlegada(id, esEmergencia);
    }

    @Auditable(value = "Agendó cita walk-in", entidad = "CITA")
    @PostMapping("/citas")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponseDTO agendarWalkIn(@Valid @RequestBody CitaRequestDTO dto) {
        return citaService.agendarCita(dto, true);
    }

    @PostMapping("/citas/{id}/reasignar")
    public CitaRecepcionResponseDTO reasignarMedico(
            @PathVariable Integer id,
            @RequestBody ReasignarMedicoRequestDTO dto) {
        return recepcionService.reasignarMedico(id, dto);
    }

    @Auditable(value = "Registró atención de emergencia", entidad = "CITA")
    @PostMapping("/pacientes/{pacienteId}/emergencia")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRecepcionResponseDTO registrarEmergenciaDirecta(
            @PathVariable Integer pacienteId,
            @Valid @RequestBody EmergenciaRequestDTO dto) {
        return recepcionService.registrarEmergenciaDirecta(pacienteId, dto);
    }

    @Auditable(value = "Registró emergencia con alta de paciente nuevo", entidad = "CITA")
    @PostMapping("/emergencia")
    @ResponseStatus(HttpStatus.CREATED)
    public CitaRecepcionResponseDTO registrarEmergenciaConAlta(
            @Valid @RequestBody EmergenciaAltaRequestDTO dto) {
        return recepcionService.registrarEmergenciaConAlta(dto);
    }
}