package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.*;
import org.umg.sistemamedicoii.service.ConsultaMedicaService;
import org.umg.sistemamedicoii.service.RecetaMedicaService;

@RestController
@RequestMapping("/api/medico")
public class ConsultaMedicaController {

    @Autowired
    private RecetaMedicaService recetaMedicaService;

    @Autowired
    private org.umg.sistemamedicoii.service.CitaService citaService;

    @Autowired
    private ConsultaMedicaService consultaMedicaService;

    @Autowired
    private org.umg.sistemamedicoii.repository.CitaRepository citaRepository;

    @GetMapping("/{medicoId}/panel")
    public PanelMedicoResponseDTO obtenerPanel(@PathVariable Integer medicoId) {
        return consultaMedicaService.obtenerPanel(medicoId);
    }

    @PostMapping("/citas/{id}/iniciar-consulta")
    public CitaConsultaResponseDTO iniciarConsulta(@PathVariable Integer id) {
        return consultaMedicaService.iniciarConsulta(id);
    }

    @PostMapping("/citas/{id}/no-asistio")
    public CitaConsultaResponseDTO marcarNoAsistio(@PathVariable Integer id) {
        return consultaMedicaService.marcarNoAsistio(id);
    }

    @PostMapping("/citas/{id}/consulta")
    public ConsultaMedicaResponseDTO guardarConsulta(
            @PathVariable Integer id,
            @RequestBody ConsultaMedicaRequestDTO dto) {
        return consultaMedicaService.guardarConsulta(id, dto);
    }

    @PostMapping("/citas/{id}/finalizar-atencion")
    public CitaConsultaResponseDTO finalizarAtencion(@PathVariable Integer id) {
        return consultaMedicaService.finalizarAtencion(id);
    }

    @PostMapping("/citas/{id}/receta")
    public RecetaMedicaResponseDTO generarReceta(
            @PathVariable Integer id,
            @RequestBody RecetaRequestDTO dto) {
        return recetaMedicaService.generarReceta(id, dto);
    }

    @PostMapping("/citas/{id}/seguimiento")
    public org.umg.sistemamedicoii.dto.CitaResponseDTO agendarSeguimiento(
            @PathVariable Integer id,
            @RequestBody org.umg.sistemamedicoii.dto.CitaRequestDTO dto) {

        org.umg.sistemamedicoii.models.Cita citaPadre = citaRepository.findById(id)
                .orElseThrow(() -> new org.umg.sistemamedicoii.exception.ResourceNotFoundException("Cita padre no encontrada"));

        dto.setCitaPadreId(id);
        dto.setPacienteId(citaPadre.getPaciente().getId());
        dto.setMedicoId(citaPadre.getMedico().getId());
        dto.setSucursalId(citaPadre.getSucursal().getId());
        dto.setEspecialidadId(citaPadre.getEspecialidad().getId());
        dto.setTipoCitaId(citaPadre.getTipoCita() != null ? citaPadre.getTipoCita().getId() : 1);

        return citaService.agendarCita(dto, true);
    }
}