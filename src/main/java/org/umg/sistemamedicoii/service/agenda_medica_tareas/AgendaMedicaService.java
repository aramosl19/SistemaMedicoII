package org.umg.sistemamedicoii.service.agenda_medica_tareas;

import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaResponseDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaResponseDTO;

import java.util.List;

public interface AgendaMedicaService {
    // Eventos
    List<EventoAgendaResponseDTO> listarEventos(Integer medicoId);
    EventoAgendaResponseDTO crearEvento(Integer medicoId, EventoAgendaRequestDTO dto);
    EventoAgendaResponseDTO actualizarEvento(Integer eventoId, EventoAgendaRequestDTO dto);
    void eliminarEvento(Integer eventoId);

    // Tareas
    List<TareaMedicaResponseDTO> listarTareas(Integer medicoId);
    TareaMedicaResponseDTO crearTarea(Integer medicoId, TareaMedicaRequestDTO dto);
    TareaMedicaResponseDTO actualizarTarea(Integer tareaId, TareaMedicaRequestDTO dto);
    TareaMedicaResponseDTO alternarEstadoTarea(Integer tareaId);
    void eliminarTarea(Integer tareaId);
}