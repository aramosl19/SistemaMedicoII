package org.umg.sistemamedicoii.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.umg.sistemamedicoii.config.security.UsuarioPrincipal;
import org.umg.sistemamedicoii.dto.UsuarioRequestDTO;
import org.umg.sistemamedicoii.dto.UsuarioResponseDTO;
import org.umg.sistemamedicoii.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponseDTO> listar() {
        return usuarioService.listar();
    }

    // FIX QA: cualquier usuario autenticado (Farmaceutico, Medico, Enfermero, etc.)
    // necesita poder leer SU PROPIO registro para datos como su sucursalId, sin
    // depender del rol ADMINISTRADOR/RECEPCIONISTA que exige GET /api/usuarios/{id}.
    // Debe declararse antes que "/{id}" para que Spring no lo confunda con un id.
    @GetMapping("/me")
    public UsuarioResponseDTO obtenerPropio(Authentication authentication) {
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        return usuarioService.obtenerPorId(principal.getUsuario().getId());
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO obtenerPorId(@PathVariable Integer id) {
        return usuarioService.obtenerPorId(id);
    }

    @PostMapping
    public UsuarioResponseDTO crear(@Valid @RequestBody UsuarioRequestDTO dto) {
        return usuarioService.crear(dto);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioRequestDTO dto) {
        return usuarioService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        usuarioService.eliminar(id);
    }

    @GetMapping("/buscar")
    public Page<UsuarioResponseDTO> buscar(
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) @Size(max = 25, message = "El criterio de búsqueda no puede superar los 25 caracteres.") String valor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return usuarioService.buscar(campo, valor, page, size);
    }
}