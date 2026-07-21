package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleDTO;
import org.umg.sistemamedicoii.service.CitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @GetMapping("/medicos-disponibles")
    public List<MedicoDisponibleDTO> medicosDisponibles(
            @RequestParam Integer sucursalId, @RequestParam Integer especialidadId){
        return citaService.listarMedicosDisponibles(sucursalId, especialidadId);
    }

    @GetMapping("/horarios-disponibles")
    public List<LocalDateTime> horariosDisponibles(
            @RequestParam Integer medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha){
        return citaService.listarHorariosDisponibles(medicoId, fecha);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponseDTO agendar(@Valid @RequestBody CitaRequestDTO dto) {
        return citaService.agendarCita(dto);
    }
}
