package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.dto.BusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.CitaRecepcionResponseDTO;
import org.umg.sistemamedicoii.dto.EmergenciaAltaRequestDTO;
import org.umg.sistemamedicoii.dto.EmergenciaRequestDTO;
import org.umg.sistemamedicoii.dto.MedicoDisponibleResponseDTO;
import org.umg.sistemamedicoii.dto.ResultadoBusquedaRecepcionResponseDTO;
import org.umg.sistemamedicoii.exception.ResourceNotFoundException;
import org.umg.sistemamedicoii.models.Cita;
import org.umg.sistemamedicoii.models.Especialidad;
import org.umg.sistemamedicoii.models.Rol;
import org.umg.sistemamedicoii.models.Sucursal;
import org.umg.sistemamedicoii.models.SucursalEspecialidad;
import org.umg.sistemamedicoii.models.TipoCita;
import org.umg.sistemamedicoii.models.Usuario;
import org.umg.sistemamedicoii.repository.CitaRepository;
import org.umg.sistemamedicoii.repository.EspecialidadRepository;
import org.umg.sistemamedicoii.repository.RolRepository;
import org.umg.sistemamedicoii.repository.SucursalEspecialidadRepository;
import org.umg.sistemamedicoii.repository.SucursalRepository;
import org.umg.sistemamedicoii.repository.TipoCitaRepository;
import org.umg.sistemamedicoii.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecepcionServiceImpl implements RecepcionService {

    private static final String ESTADO_PENDIENTE_PAGO = "Pendiente de pago";
    private static final String ESTADO_CONFIRMADA = "Confirmada";
    private static final String ESTADO_CANCELADA = "Cancelada";
    private static final String ESTADO_PACIENTE_PRESENTE = "Paciente Presente";
    private static final String ESTADO_ATENCION_FINALIZADA = "Atención Finalizada";
    private static final String ESTADO_NO_ASISTIO = "No Asistió";
    private static final String ESPECIALIDAD_EMERGENCIA = "Medicina General";
    private static final String ROL_PACIENTE = "Paciente";

    @Autowired private CitaRepository citaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SucursalRepository sucursalRepository;
    @Autowired private EspecialidadRepository especialidadRepository;
    @Autowired private TipoCitaRepository tipoCitaRepository;
    @Autowired private org.umg.sistemamedicoii.config.EstadoCitaCache estadoCache;
    @Autowired private org.umg.sistemamedicoii.repository.AuditoriaRepository auditoriaRepo;
    @Autowired private RolRepository rolRepository;
    @Autowired private SucursalEspecialidadRepository sucursalEspecialidadRepository;
    @Autowired private CitaService citaService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public BusquedaRecepcionResponseDTO buscar(Integer numeroCita, String dpi) {

        if (numeroCita == null && (dpi == null || dpi.isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un número de cita o DPI para buscar.");
        }

        if (numeroCita != null) {
            return citaRepository.findById(numeroCita)
                    .map(this::construirEncontrada)
                    .orElseGet(() -> {
                        BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
                        respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.CITA_NO_ENCONTRADA);
                        return respuesta;
                    });
        }

        Usuario paciente = usuarioRepository.findByDpi(dpi).orElse(null);

        if (paciente == null) {
            BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
            respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.PACIENTE_NO_REGISTRADO);
            return respuesta;
        }

        List<Cita> citasActivas = citaRepository
                .findByPaciente_IdAndEstado_NombreNotOrderByFechaHoraAsc(paciente.getId(), ESTADO_CANCELADA)
                .stream()
                // FIX: "Atención Finalizada" y "No Asistió" son estados terminales,
                // igual que "Cancelada" — una cita en esos estados no debe contar
                // como cita activa del paciente.
                .filter(c -> !c.getEstado().getNombre().equals(ESTADO_ATENCION_FINALIZADA)
                        && !c.getEstado().getNombre().equals(ESTADO_NO_ASISTIO))
                .toList();

        Cita citaActiva = citasActivas.stream()
                .filter(c -> c.getFechaHora().toLocalDate().equals(LocalDate.now()))
                .findFirst()
                .orElse(citasActivas.isEmpty() ? null : citasActivas.get(0));

        if (citaActiva == null) {
            BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
            respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.SIN_CITAS_ACTIVAS);
            respuesta.setPacienteNombre(paciente.getNombreCompleto());
            respuesta.setPacienteId(paciente.getId());
            return respuesta;
        }

        return construirEncontrada(citaActiva);
    }

    @Override
    public CitaRecepcionResponseDTO registrarLlegada(Integer citaId, boolean emergencia) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la cita ingresada."));

        String estadoActual = cita.getEstado().getNombre();

        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.CANCELADA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La cita fue cancelada. El paciente debe agendar una nueva cita.");
        }
        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.PENDIENTE_PAGO.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
        }
        if (org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("La llegada de este paciente ya fue registrada previamente.");
        }
        if (!org.umg.sistemamedicoii.enums.EstadoCitaEnum.CONFIRMADA.getNombreBd().equalsIgnoreCase(estadoActual)) {
            throw new IllegalArgumentException("No es posible registrar la llegada: la cita se encuentra en estado '" + estadoActual + "'.");
        }

        cita.setEstado(estadoCache.getEstado(org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE));
        cita.setHoraLlegada(LocalDateTime.now());
        cita.setEmergencia(emergencia);
        citaRepository.save(cita);

        CitaRecepcionResponseDTO respuesta = toRecepcionDTO(cita);
        respuesta.setMensaje(emergencia
                ? "Paciente " + cita.getPaciente().getNombreCompleto() + " registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales."
                : "La llegada del paciente " + cita.getPaciente().getNombreCompleto() + " ha sido registrada exitosamente. El paciente debe pasar a la sala de espera.");
        return respuesta;
    }

    @Override
    public CitaRecepcionResponseDTO reasignarMedico(Integer citaId, org.umg.sistemamedicoii.dto.ReasignarMedicoRequestDTO dto) {
        Cita cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));

        String estado = cita.getEstado().getNombre();
        String estadoConfirmada = org.umg.sistemamedicoii.enums.EstadoCitaEnum.CONFIRMADA.getNombreBd();
        String estadoPresente = org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE.getNombreBd();
        if (!estado.equals(estadoConfirmada) && !estado.equals(estadoPresente)) {
            throw new IllegalArgumentException("Solo se pueden reasignar citas Confirmadas o con Paciente Presente.");
        }
        Usuario nuevoMedico = usuarioRepository.findById(dto.getNuevoMedicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));
        if (nuevoMedico.getSucursal() == null || !nuevoMedico.getSucursal().getId().equals(cita.getSucursal().getId()) ||
                nuevoMedico.getEspecialidad() == null || !nuevoMedico.getEspecialidad().getId().equals(cita.getEspecialidad().getId())) {
            throw new IllegalArgumentException("El nuevo médico debe ser de la misma sede y especialidad.");
        }
        if (citaRepository.existsByMedicoIdAndFechaHoraAndEstado_NombreNot(
                nuevoMedico.getId(), cita.getFechaHora(), org.umg.sistemamedicoii.enums.EstadoCitaEnum.CANCELADA.getNombreBd())) {
            throw new IllegalArgumentException("El nuevo médico no tiene disponibilidad en este horario.");
        }
        Integer medicoAnteriorId = cita.getMedico().getId();
        cita.setMedico(nuevoMedico);
        citaRepository.save(cita);
        org.umg.sistemamedicoii.models.Auditoria log = new org.umg.sistemamedicoii.models.Auditoria();
        log.setAccion("REASIGNACION_MEDICO");
        log.setEntidadAfectada("CITA");
        log.setEntidadId(cita.getId());
        log.setDetalle("Médico anterior: " + medicoAnteriorId + ". Nuevo médico: " + nuevoMedico.getId() + ". Motivo: " + (dto.getMotivoReasignacion() != null ? dto.getMotivoReasignacion() : "Sin especificar"));

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
            log.setUsuarioEjecutorId(principal.getUsuario().getId());
        } else {
            log.setUsuarioEjecutorId(null);
        }

        log.setFechaHora(LocalDateTime.now());
        auditoriaRepo.save(log);
        CitaRecepcionResponseDTO res = toRecepcionDTO(cita);
        res.setMensaje("Médico reasignado correctamente.");
        return res;
    }

    // FIX CU-05 FA01: alta directa de un paciente en emergencia, sin cita previa.
    // Crea la Cita ya en estado "Paciente Presente" (llegó físicamente), marcada
    // emergencia=true, sin exigir horario ni pasar por Pendiente de pago/Confirmada.
    // Se mantiene tal cual para no romper integraciones que ya elijan
    // sucursal/especialidad/médico a mano; el flujo nuevo (con alta automática de
    // paciente) es registrarEmergenciaConAlta().
    @Override
    public CitaRecepcionResponseDTO registrarEmergenciaDirecta(Integer pacienteId, EmergenciaRequestDTO dto) {
        Usuario paciente = usuarioRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
        Usuario medico = usuarioRepository.findById(dto.getMedicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));
        Sucursal sucursal = sucursalRepository.findById(dto.getSucursalId())
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        Especialidad especialidad = especialidadRepository.findById(dto.getEspecialidadId())
                .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada."));

        TipoCita tipoCita = null;
        if (dto.getTipoCitaId() != null) {
            tipoCita = tipoCitaRepository.findById(dto.getTipoCitaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de cita no encontrado."));
        }

        return crearCitaEmergencia(paciente, medico, sucursal, especialidad, tipoCita, dto.getMotivo());
    }

    // FIX CU-05 FA01: alta automática de la cuenta del paciente cuando el DPI
    // ingresado no existe, y creación directa de la cita de emergencia en un solo
    // paso. Sede/especialidad/médico ya NO se le piden al Recepcionista: se
    // resuelven a partir de su propia sesión (su sucursal asignada), tal como pidió
    // el equipo — el documento no menciona esos datos como parte de este paso.
    //
    // Toda emergencia entra con prioridad ALTA por definición: si no ameritara
    // prioridad alta, no calificaría como emergencia. Por eso no existe (ni se
    // agrega aquí) un campo de triaje/severidad — se decidió en equipo que esa
    // distinción no aporta valor: cualquier caso que use este flujo ya se
    // considera urgente y se salta la sala de espera normal (pero NO se salta la
    // toma de signos vitales, que sigue siendo obligatoria antes de la consulta).
    @Override
    public CitaRecepcionResponseDTO registrarEmergenciaConAlta(EmergenciaAltaRequestDTO dto) {
        Usuario recepcionista = usuarioAutenticado()
                .orElseThrow(() -> new IllegalStateException("No fue posible identificar al Empleado Interno autenticado."));

        if (recepcionista.getSucursal() == null) {
            throw new IllegalArgumentException("Su usuario no tiene una sede asignada; no es posible registrar la emergencia.");
        }
        Sucursal sucursal = recepcionista.getSucursal();

        Especialidad especialidad = sucursalEspecialidadRepository.findByActivoTrue().stream()
                .filter(se -> se.getSucursal().getId().equals(sucursal.getId())
                        && ESPECIALIDAD_EMERGENCIA.equalsIgnoreCase(
                        se.getEspecialidad().getNombre() == null ? "" : se.getEspecialidad().getNombre().trim()))
                .map(SucursalEspecialidad::getEspecialidad)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay especialidad \"Medicina General\" configurada para su sede; no se puede registrar la emergencia."));

        List<MedicoDisponibleResponseDTO> medicosDisponibles =
                citaService.listarMedicosDisponibles(sucursal.getId(), especialidad.getId());
        if (medicosDisponibles.isEmpty()) {
            throw new IllegalArgumentException("No hay médicos disponibles de Medicina General en su sede en este momento.");
        }
        Usuario medico = usuarioRepository.findById(medicosDisponibles.get(0).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado."));

        Usuario paciente = usuarioRepository.findByDpi(dto.getDpi())
                .orElseGet(() -> crearPacienteMinimo(dto.getNombrePaciente(), dto.getDpi()));

        return crearCitaEmergencia(paciente, medico, sucursal, especialidad, null, dto.getMotivo());
    }

    // Alta MÍNIMA del paciente: solo lo que exige el paso 2 de FA01 (nombre y
    // DPI). correo/nombreUsuario/password son NOT NULL + UNIQUE en el esquema
    // actual, así que se rellenan con placeholders únicos basados en el DPI —
    // la persona queda activa=true para que el flujo clínico continúe de
    // inmediato, pero SIN poder iniciar sesión en el portal todavía (la
    // contraseña es aleatoria y no se le entrega a nadie).
    //
    // PENDIENTE de confirmar con Edy Ramírez: quién completa correo/teléfono/NIT
    // reales después, y si el Recepcionista o el propio paciente debe hacerlo
    // desde algún flujo de "completar mi registro". Por ahora esos tres campos
    // se guardan con el placeholder hasta que alguien los edite.
    private Usuario crearPacienteMinimo(String nombre, String dpi) {
        Rol rolPaciente = rolRepository.findByNombre(ROL_PACIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("El rol 'Paciente' no está configurado en el sistema."));

        Usuario paciente = new Usuario();
        paciente.setNombreCompleto(nombre);
        paciente.setDpi(dpi);
        paciente.setRol(rolPaciente);
        paciente.setActivo(true);
        paciente.setCorreo("dpi" + dpi + "@pendiente.hospitalelmilagro.local");
        paciente.setNombreUsuario("emergencia_" + dpi);
        paciente.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        return usuarioRepository.save(paciente);
    }

    private CitaRecepcionResponseDTO crearCitaEmergencia(Usuario paciente, Usuario medico, Sucursal sucursal,
                                                         Especialidad especialidad, TipoCita tipoCita, String motivo) {
        LocalDateTime ahora = LocalDateTime.now();

        Cita cita = new Cita();
        cita.setPaciente(paciente);
        cita.setMedico(medico);
        cita.setSucursal(sucursal);
        cita.setEspecialidad(especialidad);
        cita.setEstado(estadoCache.getEstado(org.umg.sistemamedicoii.enums.EstadoCitaEnum.PACIENTE_PRESENTE));
        cita.setFechaHora(ahora);
        cita.setHoraLlegada(ahora);
        cita.setMotivo(motivo != null && !motivo.isBlank()
                ? motivo
                : "Atención de emergencia registrada por recepción, sin cita previa.");
        cita.setTipoCita(tipoCita);
        cita.setPrecio(tipoCita != null ? tipoCita.getPrecio() : null);
        cita.setFechaCreacion(ahora);
        cita.setCreadaPorPersonalInterno(true);
        cita.setEmergencia(true);

        citaRepository.save(cita);

        CitaRecepcionResponseDTO respuesta = toRecepcionDTO(cita);
        respuesta.setMensaje("Paciente " + paciente.getNombreCompleto()
                + " registrado con prioridad de EMERGENCIA. El paciente debe pasar directamente a toma de signos vitales.");
        return respuesta;
    }

    private java.util.Optional<Usuario> usuarioAutenticado() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.umg.sistemamedicoii.config.security.UsuarioPrincipal principal) {
            return java.util.Optional.ofNullable(principal.getUsuario());
        }
        return java.util.Optional.empty();
    }

    private BusquedaRecepcionResponseDTO construirEncontrada(Cita cita) {
        BusquedaRecepcionResponseDTO respuesta = new BusquedaRecepcionResponseDTO();
        respuesta.setResultado(ResultadoBusquedaRecepcionResponseDTO.CITA_ENCONTRADA);
        respuesta.setCita(toRecepcionDTO(cita));
        return respuesta;
    }

    private CitaRecepcionResponseDTO toRecepcionDTO(Cita cita) {
        CitaRecepcionResponseDTO dto = new CitaRecepcionResponseDTO();
        dto.setId(cita.getId());
        dto.setPacienteId(cita.getPaciente().getId());
        dto.setPacienteNombre(cita.getPaciente().getNombreCompleto());
        dto.setEstadoNombre(cita.getEstado().getNombre());
        dto.setEspecialidadNombre(cita.getEspecialidad().getNombre());
        dto.setSucursalNombre(cita.getSucursal().getNombre());
        dto.setMedicoNombre(cita.getMedico() != null ? cita.getMedico().getNombreCompleto() : null);
        dto.setFechaHora(cita.getFechaHora());
        dto.setMotivo(cita.getMotivo());
        dto.setEmergencia(cita.isEmergencia());
        dto.setHoraLlegada(cita.getHoraLlegada());
        return dto;
    }
}