package org.umg.sistemamedicoii.controller.gestion_usuarios_acceso;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.UsuarioService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/portal")
public class PortalController {

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
}