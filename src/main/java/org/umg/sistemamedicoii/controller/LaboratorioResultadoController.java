package org.umg.sistemamedicoii.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.DetalleOrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.OrdenLaboratorioResponseDTO;
import org.umg.sistemamedicoii.dto.RegistrarResultadoRequestDTO;
import org.umg.sistemamedicoii.service.LaboratorioResultadoService;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorio")
public class LaboratorioResultadoController {

    @Autowired
    private LaboratorioResultadoService laboratorioResultadoService;

    @GetMapping("/ordenes")
    public List<OrdenLaboratorioResponseDTO> listarOrdenes(@RequestParam(defaultValue = "EN_PROCESO") String estado) {
        return laboratorioResultadoService.listarOrdenes(estado);
    }

    @GetMapping("/ordenes/{id}")
    public OrdenLaboratorioResponseDTO obtenerDetalle(@PathVariable Integer id) {
        return laboratorioResultadoService.obtenerDetalle(id);
    }

    @PostMapping("/examenes/{detalleId}/resultado")
    public DetalleOrdenLaboratorioResponseDTO registrarResultado(
            @PathVariable Integer detalleId,
            @RequestBody RegistrarResultadoRequestDTO dto) {
        return laboratorioResultadoService.registrarResultado(detalleId, dto);
    }



    @PostMapping("/examenes/{detalleId}/publicar")
    // TODO (seguridad pendiente, RN-CU09-02 / RNF-024): restringir este endpoint a rol
    // "SupervisorLaboratorio" (o "Administrador") una vez esté activo Spring Security.
    // registrarResultado() se mantiene abierto a rol "Personal de Laboratorio".
    public DetalleOrdenLaboratorioResponseDTO publicarResultado(@PathVariable Integer detalleId) {
        return laboratorioResultadoService.publicarResultado(detalleId);
    }
}