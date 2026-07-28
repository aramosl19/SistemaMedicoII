package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.*;
import org.umg.sistemamedicoii.service.ConsultaMedicaService;

@RestController
@RequestMapping("/api/medico")
public class ConsultaMedicaController {

    @Autowired
    private ConsultaMedicaService consultaMedicaService;

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
}