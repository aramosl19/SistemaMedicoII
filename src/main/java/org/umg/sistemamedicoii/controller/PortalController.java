package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.dto.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.RegistroExternoDTO;
import org.umg.sistemamedicoii.dto.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.VerificarDpiRequestDTO;
import org.umg.sistemamedicoii.dto.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.service.UsuarioService;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO registrarExterno(@Valid @RequestBody RegistroExternoDTO dto) {
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