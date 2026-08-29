package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.ExamenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.examenes_laboratorio.ExamenLaboratorio;
import org.umg.sistemamedicoii.models.examenes_laboratorio.Laboratorio;
import org.umg.sistemamedicoii.repository.examenes_laboratorio.LaboratorioRepository;
import org.umg.sistemamedicoii.service.examenes_laboratorio.ExamenLaboratorioService;

import java.util.List;

@RestController
@RequestMapping("/api/examenes-laboratorio")
public class ExamenLaboratorioController {

    @Autowired
    private ExamenLaboratorioService examenLaboratorioService;

    @Autowired
    private LaboratorioRepository laboratorioRepository;

    @GetMapping
    public List<ExamenLaboratorio> listar() {
        return examenLaboratorioService.listar();
    }

    @GetMapping("/{id}")
    public ExamenLaboratorio obtenerPorId(@PathVariable Integer id) {
        return examenLaboratorioService.obtenerPorId(id);
    }

    @Auditable(value = "Creó Examen de Laboratorio", entidad = "EXAMEN_LABORATORIO")
    @PostMapping
    public ExamenLaboratorio crear(@Valid @RequestBody ExamenLaboratorioRequestDTO dto) {
        ExamenLaboratorio examen = mapToEntity(new ExamenLaboratorio(), dto);
        return examenLaboratorioService.crear(examen);
    }

    @Auditable(value = "Actualizó Examen de Laboratorio", entidad = "EXAMEN_LABORATORIO")
    @PutMapping("/{id}")
    public ExamenLaboratorio actualizar(@PathVariable Integer id, @Valid @RequestBody ExamenLaboratorioRequestDTO dto) {
        ExamenLaboratorio examen = mapToEntity(new ExamenLaboratorio(), dto);
        return examenLaboratorioService.actualizar(id, examen);
    }

    @Auditable(value = "Eliminó Examen de Laboratorio", entidad = "EXAMEN_LABORATORIO")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        examenLaboratorioService.eliminar(id);
    }

    private ExamenLaboratorio mapToEntity(ExamenLaboratorio examen, ExamenLaboratorioRequestDTO dto) {
        examen.setNombre(dto.getNombre());
        examen.setDescripcion(dto.getDescripcion());
        examen.setPrecio(dto.getPrecio());
        examen.setRangoReferencia(dto.getRangoReferencia());
        examen.setUnidadMedida(dto.getUnidadMedida());

        Laboratorio laboratorio = laboratorioRepository.findById(dto.getLaboratorioId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el laboratorio con id " + dto.getLaboratorioId() + "."));
        examen.setLaboratorio(laboratorio);
        if(dto.getActivo() != null) examen.setActivo(dto.getActivo());

        return examen;
    }
}