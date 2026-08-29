package org.umg.sistemamedicoii.controller.atencion_medica_enfermeria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.*;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaResponseDTO;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.service.atencion_medica_enfermeria.ConsultaMedicaService;
import org.umg.sistemamedicoii.service.atencion_medica_enfermeria.RecetaMedicaService;
import org.umg.sistemamedicoii.service.gestion_citas_recepcion.CitaService;

@RestController
@RequestMapping("/api/medico")
public class ConsultaMedicaController {

    @Autowired
    private RecetaMedicaService recetaMedicaService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private ConsultaMedicaService consultaMedicaService;

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping("/{medicoId}/panel")
    public PanelMedicoResponseDTO obtenerPanel(@PathVariable Integer medicoId) {
        return consultaMedicaService.obtenerPanel(medicoId);
    }

    @Auditable(value = "Inició consulta médica", entidad = "CITA")
    @PostMapping("/citas/{id}/iniciar-consulta")
    public CitaConsultaResponseDTO iniciarConsulta(@PathVariable Integer id) {
        return consultaMedicaService.iniciarConsulta(id);
    }

    @Auditable(value = "Marcó cita como No Asistió", entidad = "CITA")
    @PostMapping("/citas/{id}/no-asistio")
    public CitaConsultaResponseDTO marcarNoAsistio(@PathVariable Integer id) {
        return consultaMedicaService.marcarNoAsistio(id);
    }

    @Auditable(value = "Registró consulta médica", entidad = "CONSULTA_MEDICA")
    @PostMapping("/citas/{id}/consulta")
    public ConsultaMedicaResponseDTO guardarConsulta(
            @PathVariable Integer id,
            @RequestBody ConsultaMedicaRequestDTO dto) {
        return consultaMedicaService.guardarConsulta(id, dto);
    }

    @GetMapping("/citas/{id}/consulta")
    public ConsultaMedicaResponseDTO obtenerBorrador(@PathVariable Integer id) {
        return consultaMedicaService.obtenerBorrador(id);
    }

    @Auditable(value = "Finalizó atención médica", entidad = "CITA")
    @PostMapping("/citas/{id}/finalizar-atencion")
    public CitaConsultaResponseDTO finalizarAtencion(@PathVariable Integer id) {
        return consultaMedicaService.finalizarAtencion(id);
    }

    @Auditable(value = "Generó receta médica", entidad = "RECETA_MEDICA")
    @PostMapping("/citas/{id}/receta")
    public RecetaMedicaResponseDTO generarReceta(
            @PathVariable Integer id,
            @RequestBody RecetaRequestDTO dto) {
        return recetaMedicaService.generarReceta(id, dto);
    }

    @Auditable(value = "Agendó cita de seguimiento", entidad = "CITA")
    @PostMapping("/citas/{id}/seguimiento")
    public CitaResponseDTO agendarSeguimiento(
            @PathVariable Integer id,
            @RequestBody CitaRequestDTO dto) {

        Cita citaPadre = citaRepository.findById(id)
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