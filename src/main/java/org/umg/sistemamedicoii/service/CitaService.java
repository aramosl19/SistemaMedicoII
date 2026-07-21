package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaService {
    List<MedicoDisponibleDTO> listarMedicosDisponibles(Integer sucursalId, Integer especialidadId);
    List<LocalDateTime> listarHorariosDisponibles(Integer medicoId, LocalDate fecha);
    CitaResponseDTO agendarCita(CitaRequestDTO dto);
}
