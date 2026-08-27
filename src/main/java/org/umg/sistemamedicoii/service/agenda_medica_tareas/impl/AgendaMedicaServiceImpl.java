package org.umg.sistemamedicoii.service.agenda_medica_tareas.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.EventoAgendaResponseDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaRequestDTO;
import org.umg.sistemamedicoii.dto.agenda_medica_tareas.TareaMedicaResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.agenda_medica_tareas.EventoAgenda;
import org.umg.sistemamedicoii.models.agenda_medica_tareas.TareaMedica;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;
import org.umg.sistemamedicoii.repository.agenda_medica_tareas.EventoAgendaRepository;
import org.umg.sistemamedicoii.repository.agenda_medica_tareas.TareaMedicaRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.UsuarioRepository;
import org.umg.sistemamedicoii.service.agenda_medica_tareas.AgendaMedicaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgendaMedicaServiceImpl implements AgendaMedicaService {

    @Autowired private EventoAgendaRepository eventoRepository;
    @Autowired private TareaMedicaRepository tareaRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    // Eventos
    @Override
    public List<EventoAgendaResponseDTO> listarEventos(Integer medicoId) {
        return eventoRepository.findByMedicoIdOrderByFechaInicioAsc(medicoId).stream()
                .map(this::toEventoDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EventoAgendaResponseDTO crearEvento(Integer medicoId, EventoAgendaRequestDTO dto) {
        Usuario medico = obtenerMedico(medicoId);
        validarFechasEvento(dto.getFechaInicio(), dto.getFechaFin());

        EventoAgenda evento = new EventoAgenda();
        evento.setMedico(medico);
        mapearDatosEvento(evento, dto);

        return toEventoDTO(eventoRepository.save(evento));
    }

    @Override
    public EventoAgendaResponseDTO actualizarEvento(Integer eventoId, EventoAgendaRequestDTO dto) {
        EventoAgenda evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado."));
        validarFechasEvento(dto.getFechaInicio(), dto.getFechaFin());

        mapearDatosEvento(evento, dto);
        return toEventoDTO(eventoRepository.save(evento));
    }

    @Override
    public void eliminarEvento(Integer eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new ResourceNotFoundException("Evento no encontrado.");
        }
        eventoRepository.deleteById(eventoId);
    }

    // Tareas
    @Override
    public List<TareaMedicaResponseDTO> listarTareas(Integer medicoId) {
        return tareaRepository.findByMedicoIdOrderByCompletadaAscFechaLimiteAsc(medicoId).stream()
                .map(this::toTareaDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TareaMedicaResponseDTO crearTarea(Integer medicoId, TareaMedicaRequestDTO dto) {
        Usuario medico = obtenerMedico(medicoId);

        TareaMedica tarea = new TareaMedica();
        tarea.setMedico(medico);
        mapearDatosTarea(tarea, dto);

        return toTareaDTO(tareaRepository.save(tarea));
    }

    @Override
    public TareaMedicaResponseDTO actualizarTarea(Integer tareaId, TareaMedicaRequestDTO dto) {
        TareaMedica tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));

        mapearDatosTarea(tarea, dto);
        return toTareaDTO(tareaRepository.save(tarea));
    }

    @Override
    public TareaMedicaResponseDTO alternarEstadoTarea(Integer tareaId) {
        TareaMedica tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea no encontrada."));

        tarea.setCompletada(!tarea.isCompletada());
        return toTareaDTO(tareaRepository.save(tarea));
    }

    @Override
    public void eliminarTarea(Integer tareaId) {
        if (!tareaRepository.existsById(tareaId)) {
            throw new ResourceNotFoundException("Tarea no encontrada.");
        }
        tareaRepository.deleteById(tareaId);
    }

    // Metodos Auxiliares
    private Usuario obtenerMedico(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));
        if (!"Médico".equalsIgnoreCase(usuario.getRol().getNombre())) {
            throw new IllegalArgumentException("El usuario seleccionado no tiene rol de Médico.");
        }
        return usuario;
    }

    private void validarFechasEvento(LocalDateTime inicio, LocalDateTime fin) {
        // RN-CU14-01: Validaciones de fechas
        if (inicio.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("La fecha de inicio debe ser una fecha futura o actual.");
        }
        if (fin.isBefore(inicio) || fin.isEqual(inicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private void mapearDatosEvento(EventoAgenda evento, EventoAgendaRequestDTO dto) {
        evento.setTitulo(dto.getTitulo());
        evento.setDescripcion(dto.getDescripcion());
        evento.setFechaInicio(dto.getFechaInicio());
        evento.setFechaFin(dto.getFechaFin());
        evento.setTipoEvento(dto.getTipoEvento());
        evento.setTodoElDia(dto.isTodoElDia());
    }

    private void mapearDatosTarea(TareaMedica tarea, TareaMedicaRequestDTO dto) {
        tarea.setTitulo(dto.getTitulo());
        tarea.setDescripcion(dto.getDescripcion());
        tarea.setPrioridad(dto.getPrioridad());
        tarea.setFechaLimite(dto.getFechaLimite());
        tarea.setCompletada(dto.isCompletada());

        if (dto.getFechaLimite() != null && dto.getFechaLimite().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("La fecha límite de la tarea debe ser una fecha futura.");
        }
    }

    private EventoAgendaResponseDTO toEventoDTO(EventoAgenda evento) {
        EventoAgendaResponseDTO dto = new EventoAgendaResponseDTO();
        dto.setId(evento.getId());
        dto.setMedicoId(evento.getMedico().getId());
        dto.setTitulo(evento.getTitulo());
        dto.setDescripcion(evento.getDescripcion());
        dto.setFechaInicio(evento.getFechaInicio());
        dto.setFechaFin(evento.getFechaFin());
        dto.setTipoEvento(evento.getTipoEvento());
        dto.setTodoElDia(evento.isTodoElDia());
        dto.setColor(evento.getColor());
        return dto;
    }

    private TareaMedicaResponseDTO toTareaDTO(TareaMedica tarea) {
        TareaMedicaResponseDTO dto = new TareaMedicaResponseDTO();
        dto.setId(tarea.getId());
        dto.setTitulo(tarea.getTitulo());
        dto.setDescripcion(tarea.getDescripcion());
        dto.setPrioridad(tarea.getPrioridad());
        dto.setFechaLimite(tarea.getFechaLimite());
        dto.setCompletada(tarea.isCompletada());
        return dto;
    }
}