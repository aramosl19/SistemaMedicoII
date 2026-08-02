package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.dto.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.service.AntivirusService;
import org.umg.sistemamedicoii.service.BlobStorageService;
import org.umg.sistemamedicoii.service.CitaService;
import org.springframework.web.multipart.MultipartFile;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.repository.CitaRepository;
import java.io.IOException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;
    @Autowired
    private AntivirusService antivirusService;

    // FEAT: valida contenido del PDF (vacío/contraseña/JS embebido)
    @Autowired
    private org.umg.sistemamedicoii.service.PdfContentValidationService pdfContentValidationService;

    @Autowired
    private BlobStorageService blobStorageService;

    @Autowired
    private CitaRepository citaRepository;

    @GetMapping("/medicos-disponibles")
    public List<MedicoDisponibleResponseDTO> medicosDisponibles(
            @RequestParam Integer sucursalId, @RequestParam Integer especialidadId){
        return citaService.listarMedicosDisponibles(sucursalId, especialidadId);
    }

    @GetMapping("/horarios-disponibles")
    public List<LocalDateTime> horariosDisponibles(
            @RequestParam Integer medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha){
        return citaService.listarHorariosDisponibles(medicoId, fecha);
    }

    @GetMapping("/medico/{medicoId}")
    public List<CitaResponseDTO> citasPorMedicoYRango(
            @PathVariable Integer medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return citaService.listarCitasPorMedicoYRango(medicoId, desde, hasta);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponseDTO agendar(@Valid @RequestBody CitaRequestDTO dto) {
        return citaService.agendarCita(dto, false); // siempre viene del portal (CU-03)
    }

    // Solución QA: historial completo de citas del paciente autenticado (antes
    // paciente_citas.html solo consultaba /api/caja/citas/buscar, que solo traia pendientes de pago)
    @GetMapping("/mis-citas")
    public List<CitaResponseDTO> misCitas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return citaService.listarMisCitas(principal.getUsuario().getId());
    }

    @PostMapping("/{id}/documento")
    public java.util.Map<String, String> subirDocumento(
            @PathVariable Integer id,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }
        if (!"application/pdf".equals(archivo.getContentType())) {
            throw new IllegalArgumentException("El documento debe ser un archivo PDF válido.");
        }
        if (archivo.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("El tamaño del documento excede el máximo permitido de 2 MB.");
        }

        Cita cita = citaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        byte[] bytes = archivo.getBytes();

        // 1) Validación de contenido del PDF (vacío / con contraseña / con JS embebido).
        //    Va antes del antivirus porque es una validación local y más barata, y
        //    detecta casos que ClamAV no está diseñado para marcar como "FOUND".
        pdfContentValidationService.validarContenido(bytes);

        // 2) Escaneo antivirus: si falla, aquí se corta y nunca se sube a Blob Storage
        antivirusService.escanear(bytes);

        // 3) Solo si pasó ambas validaciones, se sube al storage
        String url = blobStorageService.subir(bytes, archivo.getOriginalFilename(), id);

        cita.setDocumentoUrl(url);
        cita.setDocumentoNombreOriginal(archivo.getOriginalFilename());
        cita.setDocumentoEstado("LIMPIO");
        citaRepository.save(cita);

        return java.util.Map.of("mensaje", "Documento cargado y verificado correctamente.");
    }
}