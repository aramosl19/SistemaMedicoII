package org.umg.sistemamedicoii.controller.agenda_medica_tareas;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaResponseDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaResponseDTO;
import org.umg.sistemamedicoii.service.agenda_medica_tareas.AgendaMedicaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
public class AgendaMedicaController {

    @Autowired
    private AgendaMedicaService agendaService;

    @GetMapping("/medicos/{medicoId}/eventos")
    public List<EventoAgendaResponseDTO> listarEventos(@PathVariable Integer medicoId) {
        return agendaService.listarEventos(medicoId);
    }

    @Auditable(value = "Creó evento de agenda médica", entidad = "EVENTO_AGENDA")
    @PostMapping("/medicos/{medicoId}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearEvento(@PathVariable Integer medicoId, @Valid @RequestBody EventoAgendaRequestDTO dto) {
        EventoAgendaResponseDTO evento = agendaService.crearEvento(medicoId, dto);
        return Map.of("mensaje", "Evento creado exitosamente.", "evento", evento);
    }

    @Auditable(value = "Actualizó evento de agenda médica", entidad = "EVENTO_AGENDA")
    @PutMapping("/eventos/{eventoId}")
    public Map<String, Object> actualizarEvento(@PathVariable Integer eventoId, @Valid @RequestBody EventoAgendaRequestDTO dto) {
        EventoAgendaResponseDTO evento = agendaService.actualizarEvento(eventoId, dto);
        return Map.of("mensaje", "Evento actualizado exitosamente.", "evento", evento);
    }

    @Auditable(value = "Eliminó evento de agenda médica", entidad = "EVENTO_AGENDA")
    @DeleteMapping("/eventos/{eventoId}")
    public Map<String, String> eliminarEvento(@PathVariable Integer eventoId) {
        agendaService.eliminarEvento(eventoId);
        return Map.of("mensaje", "Evento eliminado exitosamente.");
    }

    @GetMapping("/medicos/{medicoId}/tareas")
    public List<TareaMedicaResponseDTO> listarTareas(@PathVariable Integer medicoId) {
        return agendaService.listarTareas(medicoId);
    }

    @Auditable(value = "Creó tarea médica", entidad = "TAREA_MEDICA")
    @PostMapping("/medicos/{medicoId}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearTarea(@PathVariable Integer medicoId, @Valid @RequestBody TareaMedicaRequestDTO dto) {
        TareaMedicaResponseDTO tarea = agendaService.crearTarea(medicoId, dto);
        return Map.of("mensaje", "Tarea creada exitosamente.", "tarea", tarea);
    }

    @Auditable(value = "Actualizó tarea médica", entidad = "TAREA_MEDICA")
    @PutMapping("/tareas/{tareaId}")
    public Map<String, Object> actualizarTarea(@PathVariable Integer tareaId, @Valid @RequestBody TareaMedicaRequestDTO dto) {
        TareaMedicaResponseDTO tarea = agendaService.actualizarTarea(tareaId, dto);
        return Map.of("mensaje", "Tarea actualizada exitosamente.", "tarea", tarea);
    }

    @Auditable(value = "Cambió estado de tarea médica", entidad = "TAREA_MEDICA")
    @PatchMapping("/tareas/{tareaId}/toggle")
    public TareaMedicaResponseDTO alternarEstadoTarea(@PathVariable Integer tareaId) {
        return agendaService.alternarEstadoTarea(tareaId);
    }

    @Auditable(value = "Eliminó tarea médica", entidad = "TAREA_MEDICA")
    @DeleteMapping("/tareas/{tareaId}")
    public Map<String, String> eliminarTarea(@PathVariable Integer tareaId) {
        agendaService.eliminarTarea(tareaId);
        return Map.of("mensaje", "Tarea eliminada exitosamente.");
    }
}