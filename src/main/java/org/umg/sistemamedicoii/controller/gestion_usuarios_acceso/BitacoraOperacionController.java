package org.umg.sistemamedicoii.controller.gestion_usuarios_acceso;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.BitacoraOperacionResponseDTO;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.BitacoraOperacionService;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/bitacora")
public class BitacoraOperacionController {

    @Autowired private BitacoraOperacionService bitacoraService;

    @GetMapping
    public Page<BitacoraOperacionResponseDTO> buscar(
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) String valor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaDesde,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime horaHasta,
            @RequestParam(defaultValue = "0") int pagina) {
        return bitacoraService.buscar(campo, valor, fechaDesde, fechaHasta, horaDesde, horaHasta, pagina);
    }
}