package org.umg.sistemamedicoii.service;

import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CitaService {
    List<MedicoDisponibleResponseDTO> listarMedicosDisponibles(Integer sucursalId, Integer especialidadId);
    List<LocalDateTime> listarHorariosDisponibles(Integer medicoId, LocalDate fecha);
    CitaResponseDTO agendarCita(CitaRequestDTO dto, boolean creadaPorPersonalInterno);
    // Solución CU-16 (gap del QA): las citas nunca se mostraban en el calendario porque
    // no existía un endpoint para consultarlas por médico y rango de fechas
    List<CitaResponseDTO> listarCitasPorMedicoYRango(Integer medicoId, LocalDateTime desde, LocalDateTime hasta);

    // Solución QA: historial completo de citas del paciente autenticado (antes solo
    // se mostraban las pendientes de pago vía /api/caja/citas/buscar)
    List<CitaResponseDTO> listarMisCitas(Integer pacienteId);
}