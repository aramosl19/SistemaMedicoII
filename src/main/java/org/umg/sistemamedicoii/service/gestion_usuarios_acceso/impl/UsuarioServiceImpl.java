package org.umg.sistemamedicoii.service.gestion_usuarios_acceso.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.LoginResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.RegistroExternoRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioRequestDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.UsuarioResponseDTO;
import org.umg.sistemamedicoii.dto.gestion_usuarios_accesos.VerificarDpiResponseDTO;
import org.umg.sistemamedicoii.exception.AccountLockedException;
import org.umg.sistemamedicoii.exception.DuplicateResourceException;
import org.umg.sistemamedicoii.exception.InvalidCredentialsException;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Rol;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.AuditoriaRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.RolRepository;
import org.umg.sistemamedicoii.repository.configuracion_catalogos_sistema.SucursalRepository;
import org.umg.sistemamedicoii.repository.gestion_usuarios_accesos.UsuarioRepository;
import org.umg.sistemamedicoii.service.integraciones_externas_utilidades.EmailService;
import org.umg.sistemamedicoii.service.gestion_usuarios_acceso.UsuarioService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final int MINUTOS_BLOQUEO = 15;

    @Autowired private AuditoriaRepository auditoriaRepo;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private org.umg.sistemamedicoii.config.security.JwtService jwtService;

    private void validarSucursalObligatoriaEnCreacion(UsuarioRequestDTO dto) {
        if (dto.getSucursalId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una sucursal para el usuario.");
        }
    }

    private void validarEspecialidadSiEsMedico(Rol rol, UsuarioRequestDTO dto) {
        boolean esMedico = "Médico".equalsIgnoreCase(rol.getNombre());
        if (esMedico && dto.getEspecialidadId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una especialidad para el médico.");
        }
    }

    private void validarPasswordObligatorioEnCreacion(UsuarioRequestDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("El campo Contraseña es obligatorio.");
        }
    }

    @Override
    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public UsuarioResponseDTO obtenerPorId(Integer id) {
        Usuario usuario = buscarUsuarioOlanzar(id);
        return toResponseDTO(usuario);
    }

    @Override
    public UsuarioResponseDTO crear(UsuarioRequestDTO dto) {

        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta registrada con este correo electrónico.");
        }
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())){
            throw new DuplicateResourceException(
                    "El nombre de usuario " + dto.getNombreUsuario() + " ya se encuentra registrado.");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + dto.getRolId()));

        validarSucursalObligatoriaEnCreacion(dto);
        validarEspecialidadSiEsMedico(rol, dto);
        validarPasswordObligatorioEnCreacion(dto);

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setDpi(dto.getDpi());
        usuario.setNit(dto.getNit());
        usuario.setCorreo(dto.getCorreo());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rol);

        if (dto.getSucursalId()!=null){
            Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                    .orElseThrow(()-> new ResourceNotFoundException("Sucursal no encontrada con id "+ dto.getSucursalId()));
            usuario.setSucursal(sucursal);
        }
        if (dto.getEspecialidadId()!=null){
            Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                    .orElseThrow(()-> new ResourceNotFoundException(("Especialidad no encontrada con id "+ dto.getEspecialidadId())));
            usuario.setEspecialidad(especialidad);
        }

        Usuario guardado = usuarioRepository.save(usuario);

        registrarAuditoria("USUARIO_CREADO", guardado.getId(),
                "Usuario " + guardado.getNombreUsuario() + " creado con rol " + rol.getNombre());

        return toResponseDTO(guardado);
    }

    @Override
    public UsuarioResponseDTO actualizar(Integer id, UsuarioRequestDTO dto) {
        Usuario usuario = buscarUsuarioOlanzar(id);

        if (usuarioRepository.existsByCorreoAndIdNot(dto.getCorreo(), id)) {
            throw new DuplicateResourceException("Ese correo ya está en uso por otro usuario.");
        }

        if (usuarioRepository.existsByNombreUsuarioAndIdNot(dto.getNombreUsuario(),id)){
            throw new DuplicateResourceException("Ese nombre de usuario ya está en uso.");
        }

        if (dto.getDpi() !=null && usuarioRepository.existsByDpiAndIdNot(dto.getDpi(),id)){
            throw new DuplicateResourceException("Ese DPI ya está registrado por otro usuario.");
        }

        Rol rol = rolRepository.findById(dto.getRolId())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con id " + dto.getRolId()));

        validarEspecialidadSiEsMedico(rol, dto);
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setDpi(dto.getDpi());
        usuario.setCorreo(dto.getCorreo());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setTelefono(dto.getTelefono());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rol);

        if (dto.getActivo()!=null){
            usuario.setActivo(dto.getActivo());
        }

        if(dto.getPassword()!=null&&!dto.getPassword().isEmpty()){
            usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getSucursalId() != null) {
            Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada con id " + dto.getSucursalId()));
            usuario.setSucursal(sucursal);
        }
        if (dto.getEspecialidadId() != null) {
            Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada con id " + dto.getEspecialidadId()));
            usuario.setEspecialidad(especialidad);
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        registrarAuditoria("USUARIO_ACTUALIZADO", actualizado.getId(),
                "Usuario " + actualizado.getNombreUsuario() + " actualizado");

        return toResponseDTO(actualizado);
    }

    @Override
    public void eliminar(Integer id) {
        Usuario usuario = buscarUsuarioOlanzar(id);
        Integer idUsuario = usuario.getId();
        String nombreUsuario = usuario.getNombreUsuario();

        try {
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalArgumentException(
                    "No se puede eliminar este usuario porque tiene información asociada en el sistema (citas, consultas, movimientos, etc.).");
        }

        registrarAuditoria("USUARIO_ELIMINADO", idUsuario,
                "Usuario " + nombreUsuario + " eliminado permanentemente");
    }

    @Override
    public Page<UsuarioResponseDTO> buscar(String campo, String valor, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        if (campo == null || valor == null || valor.isBlank()) {
            return usuarioRepository.findAll(pageable).map(this::toResponseDTO);
        }

        Page<Usuario> resultado = switch (campo.toLowerCase()) {
            case "id" -> {
                try {
                    yield usuarioRepository.buscarPorId(Integer.parseInt(valor), pageable);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El ID debe ser un número entero.");
                }
            }
            case "nombre" -> usuarioRepository.buscarPorNombre(valor, pageable);
            case "correo" -> usuarioRepository.buscarPorCorreo(valor, pageable);
            case "usuario" -> usuarioRepository.buscarPorNombreUsuario(valor, pageable);
            case "dpi" -> usuarioRepository.buscarPorDpi(valor, pageable);
            case "rol" -> usuarioRepository.buscarPorRol(valor, pageable);
            default -> throw new IllegalArgumentException("Campo de búsqueda no válido: " + campo);
        };

        return resultado.map(this::toResponseDTO);
    }

    @Override
    public UsuarioResponseDTO registrarExterno(RegistroExternoRequestDTO dto) {
        if (usuarioRepository.existsByCorreo(dto.getCorreo())) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta registrada con este correo electrónico.");
        }
        if (usuarioRepository.findByDpi(dto.getDpi()).isPresent()) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión.");
        }
        if (usuarioRepository.existsByNombreUsuario(dto.getNombreUsuario())) {
            throw new DuplicateResourceException("El nombre de usuario ya se encuentra registrado.");
        }

        Rol rolPaciente = rolRepository.findByNombre("Paciente")
                .orElseThrow(() -> new ResourceNotFoundException("El rol 'Paciente' no está configurado en el sistema."));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setDpi(dto.getDpi());
        usuario.setNit(dto.getNit());
        usuario.setCorreo(dto.getCorreo());
        usuario.setNombreUsuario(dto.getNombreUsuario());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setNumeroSeguro(dto.getNumeroSeguro());
        usuario.setRol(rolPaciente);
        usuario.setActivo(true);

        Usuario guardado = usuarioRepository.save(usuario);

        emailService.enviarBienvenida(guardado.getCorreo(), guardado.getNombreCompleto());

        return toResponseDTO(guardado);
    }

    @Override
    public VerificarDpiResponseDTO verificarDpi(String dpi) {
        VerificarDpiResponseDTO response = new VerificarDpiResponseDTO();

        return usuarioRepository.findByDpi(dpi)
                .map(usuario -> {
                    response.setRegistrado(true);
                    response.setRol(usuario.getRol().getNombre());
                    response.setNombreCompleto(usuario.getNombreCompleto());
                    return response;
                })
                .orElseGet(() -> {
                    response.setRegistrado(false);
                    return response;
                });
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepository.findByNombreUsuario(dto.getNombreUsuario())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contraseña incorrectos."));

        if (usuario.getBloqueadoHasta() != null && !usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
        }

        if (usuario.getBloqueadoHasta() != null && usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            int intentos = usuario.getIntentosFallidos() + 1;
            usuario.setIntentosFallidos(intentos);

            int restantes = MAX_INTENTOS_FALLIDOS - intentos;

            if (restantes <= 0) {
                usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MINUTOS_BLOQUEO));
                usuarioRepository.save(usuario);
                throw new AccountLockedException("Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.");
            }

            usuarioRepository.save(usuario);
            throw new InvalidCredentialsException(
                    "Usuario o contraseña incorrectos. Intentos restantes: " + restantes);
        }

        if (!usuario.isActivo()) {
            throw new InvalidCredentialsException("Esta cuenta se encuentra inactiva. Contacte al administrador.");
        }

        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(usuario.getId());
        response.setNombreCompleto(usuario.getNombreCompleto());
        response.setNombreUsuario(usuario.getNombreUsuario());
        response.setRol(usuario.getRol().getNombre());
        response.setToken(jwtService.generarToken(usuario.getId(), usuario.getNombreUsuario(), usuario.getRol().getNombre()));
        return response;
    }

    private Usuario buscarUsuarioOlanzar(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Usuario no encontrado con la id " + id));
    }

    private UsuarioResponseDTO toResponseDTO(Usuario u){
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(u.getId());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setDpi(u.getDpi());
        dto.setCorreo(u.getCorreo());
        dto.setNombreUsuario(u.getNombreUsuario());
        dto.setTelefono(u.getTelefono());
        dto.setNumeroSeguro(u.getNumeroSeguro());
        dto.setRolId(u.getRol()!= null ? u.getRol().getId():null);
        dto.setSucursalId(u.getSucursal()!=null? u.getSucursal().getId():null);
        dto.setEspecialidadId(u.getEspecialidad()!=null? u.getEspecialidad().getId():null);
        dto.setRolNombre(u.getRol()!= null ? u.getRol().getNombre():null);
        dto.setSucursalNombre(u.getSucursal()!=null? u.getSucursal().getNombre():null);
        dto.setEspecialidadNombre(u.getEspecialidad()!=null? u.getEspecialidad().getNombre():null);
        dto.setActivo(u.isActivo());
        return dto;
    }

    private void registrarAuditoria(String accion, Integer entidadId, String detalle) {
        org.umg.sistemamedicoii.aop.AuditoriaHelper.registrar(auditoriaRepo, accion, "USUARIO", entidadId, detalle);
    }
}