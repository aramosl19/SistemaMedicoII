package org.umg.sistemamedicoii.controller.examenes_laboratorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioRequestDTO;
import org.umg.sistemamedicoii.dto.examenes_laboratorio.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.service.examenes_laboratorio.OrdenLaboratorioService;

@RestController
@RequestMapping("/api/medico")
public class OrdenLaboratorioController {

    @Autowired
    private OrdenLaboratorioService ordenLaboratorioService;

    @Auditable(value = "Generó orden de laboratorio", entidad = "ORDEN_LABORATORIO")
    @PostMapping("/citas/{id}/orden-laboratorio")
    public OrdenLaboratorioResponseDTO generarOrden(
            @PathVariable Integer id,
            @RequestBody OrdenLaboratorioRequestDTO dto) {
        return ordenLaboratorioService.generarOrden(id, dto);
    }
}