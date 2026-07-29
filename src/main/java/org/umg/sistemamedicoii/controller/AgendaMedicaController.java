package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.EventoAgendaRequestDTO;
import org.umg.sistemamedicoii.dto.EventoAgendaResponseDTO;
import org.umg.sistemamedicoii.dto.TareaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.TareaMedicaResponseDTO;
import org.umg.sistemamedicoii.service.AgendaMedicaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agenda")
public class AgendaMedicaController {

    @Autowired
    private AgendaMedicaService agendaService;

    // Eventos

    @GetMapping("/medicos/{medicoId}/eventos")
    public List<EventoAgendaResponseDTO> listarEventos(@PathVariable Integer medicoId) {
        return agendaService.listarEventos(medicoId);
    }

    @PostMapping("/medicos/{medicoId}/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearEvento(@PathVariable Integer medicoId, @Valid @RequestBody EventoAgendaRequestDTO dto) {
        EventoAgendaResponseDTO evento = agendaService.crearEvento(medicoId, dto);
        return Map.of("mensaje", "Evento creado exitosamente.", "evento", evento);
    }

    @PutMapping("/eventos/{eventoId}")
    public Map<String, Object> actualizarEvento(@PathVariable Integer eventoId, @Valid @RequestBody EventoAgendaRequestDTO dto) {
        EventoAgendaResponseDTO evento = agendaService.actualizarEvento(eventoId, dto);
        return Map.of("mensaje", "Evento actualizado exitosamente.", "evento", evento);
    }

    @DeleteMapping("/eventos/{eventoId}")
    public Map<String, String> eliminarEvento(@PathVariable Integer eventoId) {
        agendaService.eliminarEvento(eventoId);
        return Map.of("mensaje", "Evento eliminado exitosamente.");
    }

    // Tareas

    @GetMapping("/medicos/{medicoId}/tareas")
    public List<TareaMedicaResponseDTO> listarTareas(@PathVariable Integer medicoId) {
        return agendaService.listarTareas(medicoId);
    }

    @PostMapping("/medicos/{medicoId}/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearTarea(@PathVariable Integer medicoId, @Valid @RequestBody TareaMedicaRequestDTO dto) {
        TareaMedicaResponseDTO tarea = agendaService.crearTarea(medicoId, dto);
        return Map.of("mensaje", "Tarea creada exitosamente.", "tarea", tarea);
    }

    @PutMapping("/tareas/{tareaId}")
    public Map<String, Object> actualizarTarea(@PathVariable Integer tareaId, @Valid @RequestBody TareaMedicaRequestDTO dto) {
        TareaMedicaResponseDTO tarea = agendaService.actualizarTarea(tareaId, dto);
        return Map.of("mensaje", "Tarea actualizada exitosamente.", "tarea", tarea);
    }

    @PatchMapping("/tareas/{tareaId}/toggle")
    public TareaMedicaResponseDTO alternarEstadoTarea(@PathVariable Integer tareaId) {
        return agendaService.alternarEstadoTarea(tareaId);
    }

    @DeleteMapping("/tareas/{tareaId}")
    public Map<String, String> eliminarTarea(@PathVariable Integer tareaId) {
        agendaService.eliminarTarea(tareaId);
        return Map.of("mensaje", "Tarea eliminada exitosamente.");
    }
}