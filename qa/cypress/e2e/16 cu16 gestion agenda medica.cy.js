// Ubicación sugerida: qa/cypress/e2e/16_CU14_gestion_agenda_medica.cy.js
//
// Este spec está escrito CONTRA:
//  - 16_CU_Gestion_Agenda_Medica.docx (CU-14, aunque el archivo original y los
//    comentarios del código dicen "CU-16" — es el mismo caso de uso, numeración
//    heredada; ver gap #0 pendiente de homologar, es puramente cosmético).
//  - Reglas_de_Negocio_Consolidadas.docx, sección "CU-14: Gestión de Agenda Médica".
//
// DECISIÓN CONFIRMADA: donde CU-14 y Reglas Consolidadas se contradicen campo
// por campo, se usa CU-14 como fuente de verdad. Los 3 campos que tuvieron que
// cambiar de comportamiento respecto a lo que había implementado:
//   - Evento.fechaInicio: YA NO exige ser fecha futura/actual (CU-14 solo la
//     pide obligatoria; la restricción venía de Reglas Consolidadas).
//   - Evento.descripcion: máx 500 caracteres (antes 2000, de Consolidadas).
//   - Tarea.fechaLimite: ahora obligatoria (antes opcional, de Consolidadas).
//
// GAPS REALES CORREGIDOS EN ESTA SESIÓN:
//  - FA01: click en una cita del calendario ahora abre un popover de solo
//    lectura con paciente/fecha-hora/estado (antes no tenía onclick).
//  - FA04: texto del diálogo de eliminar corregido a "¿Está seguro de eliminar
//    este evento?" (antes: "¿Liberar este horario en la agenda de citas?").
//  - Formularios de evento y tarea ahora usan novalidate + App.markInvalidFields
//    (antes mostraban el globito nativo del navegador en vez del texto rojo).
//  - FA06: el panel de tareas ahora muestra solo las tareas del día
//    seleccionado en el calendario (currentDate), con su Descripción visible
//    (antes traía TODAS las tareas del médico sin filtrar por día).
//
// PENDIENTE (no bloquea, cosmético): homologar numeración CU-14 vs "CU-16".

describe('CU-14 / RN-CU14 - Gestión de Agenda Médica', () => {
    const medicoId = 60;
    const HOY = '2026-08-20T10:00:00'; // fecha congelada con cy.clock para que el spec sea determinista

    const eventoBase = {
        id: 700,
        medicoId,
        titulo: 'Capacitación en congreso médico',
        descripcion: 'Congreso anual de cardiología.',
        fechaInicio: '2026-08-20T14:00:00',
        fechaFin: '2026-08-20T16:00:00',
        tipoEvento: 2,
        todoElDia: false,
        color: '#8b5cf6',
    };

    const citaBase = {
        id: 800,
        pacienteNombre: 'Juan Pérez',
        estadoNombre: 'Confirmada',
        fechaHora: '2026-08-20T09:00:00',
        medicoNombre: 'Dr. Ana Ramírez',
        sucursalNombre: 'Hospital El Milagro',
        especialidadNombre: 'Cardiología',
        motivo: 'Control de rutina',
    };

    const tareaBase = {
        id: 900,
        titulo: 'Revisar resultados de laboratorio',
        descripcion: 'Paciente Juan Pérez, examen de sangre.',
        prioridad: 2,
        fechaLimite: '2026-08-20T18:00:00',
        completada: false,
    };

    const tareaOtroDia = {
        id: 901,
        titulo: 'Llamar a proveedor de insumos',
        descripcion: null,
        prioridad: 0,
        fechaLimite: '2026-08-25T09:00:00',
        completada: false,
    };

    const visitarAgenda = ({ eventos = [eventoBase], citas = [citaBase], tareas = [tareaBase, tareaOtroDia] } = {}) => {
        cy.clock(new Date(HOY).getTime(), ['Date']);
        cy.intercept('GET', `/api/agenda/medicos/${medicoId}/eventos`, eventos).as('eventos');
        cy.intercept('GET', `/api/agenda/medicos/${medicoId}/tareas`, tareas).as('tareas');
        cy.intercept('GET', `/api/citas/medico/${medicoId}*`, citas).as('citas');
        cy.simularSesion({ rol: 'Médico', nombre: 'Dr. Ana Ramírez', uid: medicoId });
        cy.visit('/medico_agenda.html');
        cy.wait(['@eventos', '@tareas', '@citas']);
    };

    describe('Fix aplicado — FA01: detalle de cita en el calendario (gap #2)', () => {
        it('un clic en una cita del calendario abre un popover de solo lectura con paciente, fecha/hora y estado', () => {
            visitarAgenda();
            cy.get('.cal-event.cita').contains(citaBase.pacienteNombre).click();
            cy.get('#modal-cita-detalle').should('be.visible');
            cy.get('#cd-paciente').should('have.text', citaBase.pacienteNombre);
            cy.get('#cd-estado').should('have.text', citaBase.estadoNombre);
            cy.get('#cd-fecha').invoke('text').should('not.be.empty');
        });

        it('el popover de cita se puede cerrar sin llamar a ningún endpoint de edición', () => {
            visitarAgenda();
            cy.get('.cal-event.cita').contains(citaBase.pacienteNombre).click();
            cy.get('#modal-cita-detalle').should('be.visible');
            cy.contains('#modal-cita-detalle button', 'Cerrar').click();
            cy.get('#modal-cita-detalle').should('have.class', 'd-none');
        });

        it('un clic en un evento (bloqueo) sigue abriendo el modal de edición, como antes', () => {
            visitarAgenda();
            cy.get('.cal-event.bloqueo').contains(eventoBase.titulo).click();
            cy.get('#modal-evento').should('be.visible');
            cy.get('#e-tit').should('have.value', eventoBase.titulo);
        });
    });

    describe('Fix aplicado — FA04: texto del diálogo de eliminar evento (gap #3)', () => {
        it('el diálogo de confirmación usa el texto exacto del documento', () => {
            visitarAgenda();
            cy.get('.cal-event.bloqueo').contains(eventoBase.titulo).click();
            cy.contains('#btn-liberar', 'Liberar Horario').click();
            cy.get('#app-confirm-titulo').should('have.text', '¿Eliminar evento?');
            cy.get('#app-confirm-mensaje').should('have.text', '¿Está seguro de eliminar este evento?');
            cy.get('#app-confirm-aceptar').should('have.text', 'Eliminar');
        });

        it('al confirmar, borra el evento y muestra el mensaje de éxito correcto', () => {
            visitarAgenda();
            cy.intercept('DELETE', `/api/agenda/eventos/${eventoBase.id}`, { mensaje: 'Evento eliminado exitosamente.' }).as('borrar');
            cy.intercept('GET', `/api/agenda/medicos/${medicoId}/eventos`, []).as('eventosVacios');
            cy.get('.cal-event.bloqueo').contains(eventoBase.titulo).click();
            cy.contains('#btn-liberar', 'Liberar Horario').click();
            cy.get('#app-confirm-aceptar').click();
            cy.wait('@borrar');
            cy.get('#msg').should('have.text', 'Evento eliminado exitosamente.');
            cy.get('#modal-evento').should('have.class', 'd-none');
        });
    });

    describe('Fix aplicado — novalidate + validación en rojo (gap #4)', () => {
        it('el formulario de evento no depende del globito nativo del navegador; marca los campos en rojo', () => {
            visitarAgenda();
            cy.get('#f-ev').should('have.attr', 'novalidate');
            cy.get('[onclick="abrirEventoModal()"]').click();
            cy.get('#e-tit').clear();
            cy.get('#f-ev button[type="submit"]').click();
            cy.get('#e-tit').should('have.class', 'is-invalid');
            cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
        });

        it('el formulario de tarea no depende del globito nativo del navegador; marca los campos en rojo', () => {
            visitarAgenda();
            cy.get('#f-ta').should('have.attr', 'novalidate');
            cy.contains('button', '+ Nueva').click();
            cy.get('#t-tit').type('Título válido');
            // fecha límite queda vacía a propósito: ahora es obligatoria (RN-CU14-02)
            cy.get('#f-ta button[type="submit"]').click();
            cy.get('#t-lim').should('have.class', 'is-invalid');
            cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
        });
    });

    describe('Fix aplicado — FA06: panel de tareas filtrado por día seleccionado (gap #5)', () => {
        it('el panel solo muestra las tareas cuya fecha límite cae en el día que se está viendo', () => {
            visitarAgenda();
            cy.get('#l-ta').should('contain.text', tareaBase.titulo);
            cy.get('#l-ta').should('not.contain.text', tareaOtroDia.titulo);
        });

        it('cada tarjeta de tarea ahora muestra la Descripción, además del título', () => {
            visitarAgenda();
            cy.get('#l-ta').should('contain.text', tareaBase.descripcion);
        });

        it('el contador de tareas pendientes del botón toggle cuenta solo las del día seleccionado', () => {
            visitarAgenda();
            cy.get('#contador-tareas').should('have.text', '1'); // solo tareaBase es de hoy
        });

        it('al navegar a otro día, el panel se refresca mostrando las tareas de ese nuevo día', () => {
            visitarAgenda();
            cy.get('[onclick="cambiarVista(\'dia\')"]').click();
            cy.get('#l-ta').should('contain.text', tareaBase.titulo);

            cy.intercept('GET', `/api/citas/medico/${medicoId}*`, []).as('citasSiguienteDia');
            cy.get('[onclick="navegarSiguiente()"]').click();
            cy.wait('@citasSiguienteDia');

            cy.get('#l-ta').should('not.contain.text', tareaBase.titulo);
            cy.get('#l-ta').should('contain.text', 'No hay tareas que coincidan para este día.');
        });

        it('muestra una etiqueta con la fecha del día que se está viendo', () => {
            visitarAgenda();
            cy.get('#fecha-tareas-panel').invoke('text').should('not.be.empty');
        });
    });

    describe('RN-CU14-01: validación de datos de evento (CU-14 como fuente de verdad)', () => {
        it('la fecha de inicio YA NO exige ser futura o actual (antes lo hacía, tomado de Consolidadas)', () => {
            visitarAgenda();
            cy.get('[onclick="abrirEventoModal()"]').click();
            cy.get('#e-tit').type('Evento con fecha pasada');
            cy.get('#e-tip').select('2');
            cy.get('#e-ini').invoke('val', '2026-01-01T08:00');
            cy.get('#e-fin').invoke('val', '2026-01-01T09:00');
            cy.intercept('POST', `/api/agenda/medicos/${medicoId}/eventos`, {
                statusCode: 201,
                body: {
                    mensaje: 'Evento creado exitosamente.',
                    evento: { ...eventoBase, id: 701, titulo: 'Evento con fecha pasada', fechaInicio: '2026-01-01T08:00:00', fechaFin: '2026-01-01T09:00:00' },
                },
            }).as('crear');
            cy.get('#f-ev button[type="submit"]').click();
            cy.wait('@crear');
            cy.get('#msg').should('contain.text', 'Evento creado exitosamente.');
        });

        it('la fecha de fin sigue siendo obligatoriamente posterior a la de inicio', () => {
            visitarAgenda();
            cy.get('[onclick="abrirEventoModal()"]').click();
            cy.get('#e-tit').type('Evento con fechas invertidas');
            cy.get('#e-tip').select('0');
            cy.get('#e-ini').invoke('val', '2026-08-21T10:00');
            cy.get('#e-fin').invoke('val', '2026-08-21T09:00');
            cy.intercept('POST', `/api/agenda/medicos/${medicoId}/eventos`, {
                statusCode: 400,
                body: { error: 'La fecha de fin debe ser posterior a la fecha de inicio.' },
            }).as('crearInvalido');
            cy.get('#f-ev button[type="submit"]').click();
            cy.wait('@crearInvalido');
            cy.get('#msg').should('have.text', 'La fecha de fin debe ser posterior a la fecha de inicio.');
        });

        it('la descripción del evento tiene un máximo de 500 caracteres (antes 2000, de Consolidadas)', () => {
            visitarAgenda();
            cy.get('[onclick="abrirEventoModal()"]').click();
            cy.get('#e-desc').should('have.attr', 'maxlength', '500');
        });

        it('el tipo de evento sigue siendo numérico con las 5 opciones de CU-14 (Reunión/Descanso/Capacitación/Personal/Otro)', () => {
            visitarAgenda();
            cy.get('[onclick="abrirEventoModal()"]').click();
            cy.get('#e-tip option').should('have.length', 5);
            ['0', '1', '2', '3', '4'].forEach(v => cy.get(`#e-tip option[value="${v}"]`).should('exist'));
        });

        it('el color del evento sigue sin ser configurable por el usuario (siempre violeta #8b5cf6)', () => {
            visitarAgenda();
            cy.get('#f-ev').find('[id*="color" i]').should('not.exist');
            cy.get('.cal-event.bloqueo').should('have.attr', 'style').and('include', eventoBase.color);
        });
    });

    describe('RN-CU14-02: validación de datos de tarea (CU-14 como fuente de verdad)', () => {
        it('la fecha límite de la tarea ahora es obligatoria en el formulario', () => {
            visitarAgenda();
            cy.contains('button', '+ Nueva').click();
            cy.get('#t-lim').should('have.attr', 'required');
        });

        it('el backend rechaza una tarea sin fecha límite (RN-CU14-02)', () => {
            visitarAgenda();
            cy.contains('button', '+ Nueva').click();
            cy.get('#t-tit').type('Tarea sin fecha límite');
            cy.get('#t-lim').invoke('removeAttr', 'required'); // se salta el HTML5 para probar el backend
            cy.intercept('POST', `/api/agenda/medicos/${medicoId}/tareas`, {
                statusCode: 400,
                body: { fechaLimite: 'La fecha límite de la tarea es obligatoria.' },
            }).as('crearTareaInvalida');
            cy.get('#f-ta button[type="submit"]').click();
            cy.wait('@crearTareaInvalida');
            cy.get('#msg').should('have.text', 'La fecha límite de la tarea es obligatoria.');
        });

        it('la prioridad sigue teniendo las 3 opciones de CU-14 (Baja/Normal/Alta)', () => {
            visitarAgenda();
            cy.contains('button', '+ Nueva').click();
            cy.get('#t-pri option').should('have.length', 3);
        });

        it('el estado de la tarea sigue siendo booleano (Pendiente/Completada), sin "En progreso"', () => {
            visitarAgenda();
            cy.get(`[onclick="toggleT(${tareaBase.id})"]`).should('exist');
            cy.get('#filtro-tareas option').should('have.length', 3);
            cy.get('#filtro-tareas option[value="ALL"]').should('exist');
            cy.get('#filtro-tareas option[value="PENDING"]').should('exist');
            cy.get('#filtro-tareas option[value="DONE"]').should('exist');
        });
    });

    describe('Pendiente (no autocorregible, cosmético)', () => {
        it.skip('gap #0: homologar la numeración CU-14 vs "CU-16" en comentarios/nombre de archivo del código', () => {
            // No bloquea nada funcional; queda a criterio de Edy/el equipo.
        });
    });
});