package org.umg.sistemamedicoii.controller.gestion_usuarios_acceso;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.UsuarioService;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private static final Logger logger = LoggerFactory.getLogger(PortalController.class);

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO registrarExterno(@Valid @RequestBody RegistroExternoRequestDTO dto) {
        return usuarioService.registrarExterno(dto);
    }

    @PostMapping("/verificar-dpi")
    public VerificarDpiResponseDTO verificarDpi(@Valid @RequestBody VerificarDpiRequestDTO dto) {
        return usuarioService.verificarDpi(dto.getDpi());
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return usuarioService.login(dto);
    }

    @PostMapping("/descartar-registro")
    public ResponseEntity<Void> descartarRegistro(@RequestBody(required = false) Map<String, Object> datosFormulario) {
        logger.debug("Registro descartado por el usuario. Datos ingresados antes de descartar: {}", datosFormulario);
        return ResponseEntity.noContent().build();
    }
}