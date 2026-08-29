package org.umg.sistemamedicoii.models.gestion_citas_recepcion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Especialidad;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.EstadoCita;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.Sucursal;
import org.umg.sistemamedicoii.models.configuracion_catalogos_sistema.TipoCita;
import org.umg.sistemamedicoii.models.gestion_usuarios_accesos.Usuario;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "paciente_id",nullable = false)
    private Usuario paciente;

    @ManyToOne
    @JoinColumn(name = "medico_id", nullable = false)
    private Usuario medico;

    @ManyToOne
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @ManyToOne
    @JoinColumn(name = "tipo_cita_id", nullable = true) // true por ahora para evitar errores con citas de prueba que ya tengas
    private TipoCita tipoCita;

    @Column(name = "cita_padre_id", nullable = true)
    private Integer citaPadreId;

    @Column(name = "tipo_seguimiento", nullable = true, length = 100)
    private String tipoSeguimiento;

    // Solución CU-12 (gap del QA): RN-CU11-03 exige motivo del seguimiento Y prioridad;
    // el formulario solo pedía motivo, faltaba este campo
    @Column(name = "prioridad_seguimiento", nullable = true, length = 20)
    private String prioridadSeguimiento;

    @ManyToOne
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoCita estado;

    @Column (nullable = false)
    private LocalDateTime fechaHora;

    @Column (nullable = false, length = 2000)
    private String motivo;

    @Column (nullable = true)
    private LocalDateTime reservadaHasta;

    @Column(nullable = true)
    private LocalDateTime horaLlegada;

    @Column(nullable = false)
    private boolean emergencia = false;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private boolean creadaPorPersonalInterno = false;

    @Column(nullable = true, precision = 10, scale = 2) // true por ahora, por si tienes datos viejos
    private java.math.BigDecimal precio;

    // CU-03: documento/referencia PDF adjunto por el paciente al agendar
    @Column(name = "documento_url", nullable = true, length = 500)
    private String documentoUrl;

    @Column(name = "documento_nombre_original", nullable = true, length = 255)
    private String documentoNombreOriginal;

    @Column(name = "documento_estado", nullable = true, length = 20)
    private String documentoEstado; // LIMPIO, RECHAZADO

    @Column(name = "recordatorio_seguimiento_enviado", nullable = false, columnDefinition = "boolean default false")
    private boolean recordatorioSeguimientoEnviado = false;
}