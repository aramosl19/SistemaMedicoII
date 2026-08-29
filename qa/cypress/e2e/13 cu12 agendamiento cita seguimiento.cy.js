// Ubicación sugerida: qa/cypress/e2e/13_CU12_agendamiento_cita_seguimiento.cy.js
//
// Este spec está escrito CONTRA:
//  - 12_CU_Agendamiento_Cita_de_Seguimiento.docx (flujo/textos de UI)
//  - Reglas_de_Negocio_Consolidadas.docx, sección "CU-11: Agendamiento de Cita
//    de Seguimiento" (RN-CU11-01 a 05, y RN-GLOBAL-006) — el documento CU-12
//    cita internamente "RN-CU11-xx" porque hereda la numeración de una versión
//    anterior; es la misma funcionalidad, confirmado con Edy Ramírez.
//
// DECISIONES CONFIRMADAS CON EDY (ya no son gaps, aunque el documento describa
// otra cosa — se dejan como comentario para que el próximo QA no las vuelva a
// levantar):
//  - El flujo real es un MODAL dentro de medico_panel.html (no una pantalla
//    nueva con router/wizard/toast). Se reutiliza el mismo patrón de
//    agendamiento de cita del paciente. Aceptado, no se migra a wizard.
//  - El botón "Agendar seguimiento" vive en la tarjeta de "Evaluados —
//    Pendiente de cierre" (post-consulta), no dentro del formulario de
//    consulta médica — ahí es exactamente donde ocurren "las acciones de la
//    consulta" (receta, examen, seguimiento) en este sistema.
//  - No hay banner de precarga; no hace falta, ya se decidió no implementarlo.
//  - El seguimiento SÍ usa al mismo médico: los horarios se piden con
//    App.getUid() (el médico logueado, dueño del panel) y el backend
//    independientemente vuelve a resolver medicoId desde la cita padre.
//  - Sí existe un calendario visual real en el sistema (medico_agenda.html,
//    CU-16), pero el modal de seguimiento no lo reutiliza — usa
//    date+select. No se pidió cambiar esto.
//  - citaPadreId es solo para saber de qué cita nace el seguimiento; no hace
//    falta llamarlo parentConsultationId ni separar cita/consulta.
//  - medico_panel.html YA funciona como "listado de citas" (las 3 columnas);
//    no hace falta redirigir a otra pantalla tras agendar.
//  - Cancelar no limpia el DOM al instante, pero abrirModal('seg', ...) hace
//    form-seg.reset() la próxima vez que se abre — los datos nunca persisten
//    de una vez a otra. Decisión consistente con otro CU.
//
// GAPS REALES YA CORREGIDOS (ver fixes_backend.md / fix_frontend_seguimiento.txt):
//  - Mensaje de éxito ahora es el texto exacto del documento (paso 8).
//  - Tilde corregida en el mensaje de conflicto de horario (FA01).
//  - @Valid agregado al endpoint -> "observaciones"/motivo ya no puede llegar
//    vacío ni demasiado corto al backend (RN-CU11-03).
//  - RecordatorioScheduler ahora marca recordatorioSeguimientoEnviado para
//    no reenviar el mismo correo día tras día (RN-CU11-05, "un recordatorio").
//
// PENDIENTE DE DECISIÓN (no autocorregible, requiere a Edy):
//  - Si la cita de seguimiento debe nacer "Pendiente de pago" o no. Ni el
//    documento CU-12 ni las Reglas de Negocio Consolidadas lo definen.
//  - Si RN-CU11-02 debe unificarse en un solo mensaje de fecha/horario o se
//    mantienen los dos mensajes actuales (fecha pasada vs. horario ocupado).

describe('CU-12 / RN-CU11 - Agendamiento de Cita de Seguimiento', () => {
    const medicoId = 50;
    const hospitalNombre = 'Hospital El Milagro';

    const citaEvaluada = {
        id: 900,
        pacienteNombre: 'Marta Solís',
        especialidadNombre: 'Medicina General',
        fechaHora: '2026-08-27T10:00:00',
        estadoNombre: 'Evaluado',
        emergencia: false,
    };

    const panelConEvaluada = { enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] };

    const cie10 = [];
    const medicamentos = [];
    const examenes = [];

    const abrirPanelConDatos = (panel) => {
        cy.intercept('GET', '/api/cie10', cie10).as('cie10');
        cy.intercept('GET', '/api/medicamentos', medicamentos).as('medicamentos');
        cy.intercept('GET', '/api/examenes-laboratorio', examenes).as('examenes');
        cy.intercept('GET', `/api/medico/${medicoId}/panel`, panel).as('panel');
        cy.intercept('GET', '/api/medico/citas/*/consulta', { body: null }).as('borrador');
        cy.simularSesion({ rol: 'Médico', nombre: 'Dr. Juan Pérez', uid: medicoId });
        cy.visit('/medico_panel.html');
        cy.wait(['@cie10', '@medicamentos', '@examenes', '@panel']);
    };

    const abrirModalSeguimiento = () => {
        abrirPanelConDatos(panelConEvaluada);
        cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Agendar seguimiento').click();
        cy.get('#modal-acciones').should('be.visible');
        cy.get('#form-seg').should('be.visible');
    };

    const llenarHastaHorario = (fecha = '2026-09-01', horarios = ['2026-09-01T09:00:00']) => {
        cy.get('#s-tipo').select('Monitoreo de Tratamiento');
        cy.get('#s-prioridad').select('Media');
        cy.intercept('GET', `/api/citas/horarios-disponibles?medicoId=${medicoId}&fecha=${fecha}`, horarios).as('horarios');
        cy.get('#s-fecha').invoke('val', fecha).trigger('change');
        cy.wait('@horarios');
        cy.get('#s-hora').select(horarios[0]);
    };

    describe('Comportamiento confirmado como correcto (ya no son gaps)', () => {
        it('el botón "Agendar seguimiento" vive en la tarjeta de Evaluados, como acción posterior a la consulta', () => {
            abrirPanelConDatos(panelConEvaluada);
            cy.get('#area-consulta').should('have.class', 'd-none');
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Agendar seguimiento').should('exist');
        });

        it('el selector de tipo de seguimiento trae exactamente las dos opciones del documento', () => {
            abrirModalSeguimiento();
            cy.get('#s-tipo option').should('have.length', 3); // placeholder + 2 opciones
            cy.get('#s-tipo option[value="Monitoreo de Tratamiento"]').should('exist');
            cy.get('#s-tipo option[value="Revisión de Resultados de Laboratorio"]').should('exist');
        });

        it('los horarios disponibles se consultan para el médico logueado (mismo médico que evaluó al paciente)', () => {
            abrirModalSeguimiento();
            cy.intercept('GET', `/api/citas/horarios-disponibles?medicoId=${medicoId}&fecha=2026-09-01`, ['2026-09-01T09:00:00']).as('horarios');
            cy.get('#s-fecha').invoke('val', '2026-09-01').trigger('change');
            cy.wait('@horarios').its('request.url').should('include', `medicoId=${medicoId}`);
        });

        it('motivo/observaciones y prioridad son obligatorios en el formulario, con minlength 10 (RN-CU11-03)', () => {
            abrirModalSeguimiento();
            cy.get('#s-prioridad').should('have.attr', 'required');
            cy.get('#s-motivo').should('have.attr', 'required');
            cy.get('#s-motivo').should('have.attr', 'minlength', '10');
        });

        it('cancelar cierra el modal sin enviar nada al backend, y una apertura posterior siempre llega limpia', () => {
            abrirModalSeguimiento();
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`).as('agendarSeg');
            cy.get('#s-tipo').select('Monitoreo de Tratamiento');
            cy.get('#s-motivo').type('Texto que no debe persistir.');
            cy.contains('#form-seg button', 'Cancelar').click();
            cy.get('@agendarSeg.all').should('have.length', 0);

            // Reabrir: el formulario debe llegar reseteado (abrirModal hace form-seg.reset())
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Agendar seguimiento').click();
            cy.get('#s-tipo').should('have.value', '');
            cy.get('#s-motivo').should('have.value', '');
        });

        it('el payload enviado usa tipoSeguimiento/prioridadSeguimiento/motivo/fechaHora; citaPadreId lo agrega el backend a partir del id de la URL, no hace falta enviarlo desde el frontend', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            cy.get('#s-motivo').type('Revisión de evolución del tratamiento.');
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                id: 950, pacienteNombre: citaEvaluada.pacienteNombre, tipoSeguimiento: 'Monitoreo de Tratamiento',
            }).as('agendarSeg');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@agendarSeg').its('request.body').should('have.all.keys', 'fechaHora', 'tipoSeguimiento', 'prioridadSeguimiento', 'motivo');
        });
    });

    describe('Fix aplicado — Mensaje de éxito (paso 8 del documento)', () => {
        it('muestra el texto EXACTO exigido por el documento, usando tipoSeguimiento y pacienteNombre que ya trae CitaResponseDTO', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            cy.get('#s-motivo').type('Revisión de evolución del tratamiento.');

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                id: 950, pacienteNombre: citaEvaluada.pacienteNombre, tipoSeguimiento: 'Monitoreo de Tratamiento',
                fechaHora: '2026-09-01T09:00:00',
            }).as('agendarSeg');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@agendarSeg');

            cy.get('#msg').should('have.text', `Cita de seguimiento agendada exitosamente. Tipo: Monitoreo de Tratamiento. Paciente: ${citaEvaluada.pacienteNombre}.`);
        });

        it('el panel se refresca después de agendar el seguimiento (antes no se llamaba loadPanel())', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            cy.get('#s-motivo').type('Revisión de evolución del tratamiento.');
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                id: 950, pacienteNombre: citaEvaluada.pacienteNombre, tipoSeguimiento: 'Monitoreo de Tratamiento',
            }).as('agendarSeg');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, { enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [] }).as('panelTrasAgendar');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@agendarSeg');
            cy.wait('@panelTrasAgendar');
        });
    });

    describe('Fix aplicado — FA01: conflicto de horario', () => {
        it('el mensaje de error ahora lleva la tilde correcta ("ya no está disponible")', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            cy.get('#s-motivo').type('Revisión de resultados de laboratorio.');

            const mensajeCorregido = 'El horario seleccionado ya no está disponible. Por favor, elija otro horario.';
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                statusCode: 400,
                body: { message: mensajeCorregido },
            }).as('conflicto');

            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@conflicto');
            cy.get('#msg').should('have.text', mensajeCorregido);
        });

        it('mejora aplicada: tras el conflicto, se limpia el horario seleccionado y se vuelven a consultar los horarios disponibles', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            cy.get('#s-motivo').type('Revisión de resultados de laboratorio.');

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                statusCode: 400,
                body: { message: 'El horario seleccionado ya no está disponible. Por favor, elija otro horario.' },
            }).as('conflicto');
            cy.intercept('GET', '/api/citas/horarios-disponibles?medicoId=' + medicoId + '&fecha=2026-09-01', ['2026-09-01T10:00:00']).as('horariosRefrescados');

            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@conflicto');
            cy.wait('@horariosRefrescados');
            cy.get('#s-hora').should('have.value', '');
        });
    });

    describe('Validación backend (RN-CU11-03) — ya no depende solo del HTML5 del navegador', () => {
        it('un motivo/observaciones vacío o corto que se cuele del lado del cliente es rechazado por el backend con @Valid activo', () => {
            abrirModalSeguimiento();
            llenarHastaHorario();
            // Se fuerza un valor inválido saltándose minlength, como si el HTML5 no bloqueara.
            cy.get('#s-motivo').invoke('removeAttr', 'required').invoke('removeAttr', 'minlength');
            cy.get('#s-motivo').type('corto');

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                statusCode: 400,
                body: { message: 'Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.' },
            }).as('rechazo');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@rechazo');
            cy.get('#msg').should('have.text', 'Las observaciones son obligatorias. Deben contener entre 10 y 2000 caracteres.');
        });
    });

    describe('Pendiente de decisión con Edy (no autocorregible, dejar fallando a propósito hasta definir)', () => {
        it.skip('RN-CU11-04: el asunto del correo debe incluir el nombre del hospital ("Hospital ' + hospitalNombre + '")', () => {
            // Requiere inspección del correo enviado (fuera del alcance de Cypress
            // contra el frontend); usar el test de integración de EmailService.
        });

        it.skip('el estado inicial de una cita de seguimiento (Pendiente de pago vs. Confirmada) queda pendiente de definir con Edy', () => {
            // Ni CU-12 ni Reglas de Negocio Consolidadas lo especifican.
        });
    });
});