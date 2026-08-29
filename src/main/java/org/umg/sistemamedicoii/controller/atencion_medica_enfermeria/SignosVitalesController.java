package org.umg.sistemamedicoii.controller.atencion_medica_enfermeria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.CitaEnfermeriaResponseDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.SignosVitalesRequestDTO;
import org.umg.sistemamedicoii.dto.atencion_medica_enfermeria.SignosVitalesResponseDTO;
import org.umg.sistemamedicoii.service.atencion_medica_enfermeria.SignosVitalesService;

import java.util.List;

@RestController
@RequestMapping("/api/enfermeria")
public class SignosVitalesController {

    @Autowired
    private SignosVitalesService signosVitalesService;

    @GetMapping("/citas/en-espera")
    public List<CitaEnfermeriaResponseDTO> listarEnEspera() {
        return signosVitalesService.listarCitasPresentes();
    }

    @Auditable(value = "Llamó al paciente a consulta", entidad = "CITA")
    @PostMapping("/citas/{id}/llamar")
    public CitaEnfermeriaResponseDTO llamarPaciente(@PathVariable Integer id) {
        return signosVitalesService.llamarPaciente(id);
    }

    @Auditable(value = "Registró signos vitales", entidad = "SIGNOS_VITALES")
    @PostMapping("/citas/{id}/signos-vitales")
    public SignosVitalesResponseDTO registrarSignosVitales(
            @PathVariable Integer id,
            @RequestBody SignosVitalesRequestDTO dto) {
        return signosVitalesService.registrarSignosVitales(id, dto);
    }
}