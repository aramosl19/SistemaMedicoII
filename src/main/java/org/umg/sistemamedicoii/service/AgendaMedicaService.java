package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.EventoAgendaRequestDTO;
import org.umg.sistemamedicoii.dto.EventoAgendaResponseDTO;
import org.umg.sistemamedicoii.dto.TareaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.TareaMedicaResponseDTO;

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