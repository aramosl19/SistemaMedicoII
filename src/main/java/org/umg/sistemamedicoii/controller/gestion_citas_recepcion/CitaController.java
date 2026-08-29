package org.umg.sistemamedicoii.controller.gestion_citas_recepcion;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.aop.Auditable;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.CitaResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_citas_recepcion.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.AntivirusService;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.BlobStorageService;
import org.umg.sistemamedicoii.service.gestion_citas_recepcion.CitaService;
import org.springframework.web.multipart.MultipartFile;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.gestion_citas_recepcion.Cita;
import org.umg.sistemamedicoii.repository.gestion_cita_recepcion.CitaRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.PdfContentValidationService;

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

    @Autowired
    private PdfContentValidationService pdfContentValidationService;

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

    @Auditable(value = "Agendó cita médica", entidad = "CITA")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CitaResponseDTO agendar(@Valid @RequestBody CitaRequestDTO dto) {
        return citaService.agendarCita(dto, false);
    }

    @GetMapping("/mis-citas")
    public List<CitaResponseDTO> misCitas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return citaService.listarMisCitas(principal.getUsuario().getId());
    }

    @Auditable(value = "Subió documento adjunto a cita", entidad = "CITA")
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

        pdfContentValidationService.validarContenido(bytes);
        antivirusService.escanear(bytes);
        String url = blobStorageService.subir(bytes, archivo.getOriginalFilename(), id);

        cita.setDocumentoUrl(url);
        cita.setDocumentoNombreOriginal(archivo.getOriginalFilename());
        cita.setDocumentoEstado("LIMPIO");
        citaRepository.save(cita);

        return java.util.Map.of("mensaje", "Documento cargado y verificado correctamente.");
    }
}