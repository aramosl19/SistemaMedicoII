// Ubicación en tu proyecto: qa/cypress/e2e/09_CU08_consulta_medica.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 8_CU_Consulta_Medica.docx.
// Donde medico_panel.html / el backend real se desvían del texto/las reglas
// del documento, la prueba queda tal cual el documento lo exige y por lo
// tanto VA A FALLAR — con un comentario "NOTA GAP" explicando la diferencia
// encontrada. No se ajustó ningún selector, texto esperado ni mock para
// maquillar el resultado.
//
// Gaps de FRONTEND encontrados entre el documento y medico_panel.html:
//
//  A) Doc (paso 1): las tres secciones del panel deben titularse "En Espera
//     de Consulta", "En Consulta Médica" y "Evaluados — Pendiente de
//     cierre". Los títulos reales son "En espera (triage completado)",
//     "Consulta activa" y "Evaluados (pendiente de cierre)".
//  B) Doc (paso 1): cada tarjeta debe mostrar número de cita, nombre del
//     paciente, especialidad, fecha y estado. Las tarjetas reales solo
//     muestran el nombre del paciente y la hora (ni fecha completa, ni
//     número de cita, ni especialidad, ni estado como texto).
//  C) Doc (paso 1): la indicación de prioridad debe decir "Emergencia". La
//     tarjeta real muestra un badge que dice "URGENCIA".
//  D) Doc (paso 3): el botón para abrir la consulta ya iniciada debe decir
//     "Ver / Completar Consulta". El botón real dice "Continuar evaluación".
//  E) Doc (paso 4): el formulario debe incluir un campo "Notas Adicionales".
//     No existe ningún campo de notas adicionales en el HTML real, y el
//     payload que se envía al backend tampoco incluye notasAdicionales en
//     absoluto (aunque el DTO/entidad del backend sí lo soportan).
//  F) Doc (paso 9): el botón para guardar debe decir "Guardar Consulta". El
//     botón real dice "Guardar historial clínico".
//  G) Doc (paso 10): al finalizar la consulta, el sistema debe mostrar el
//     mensaje que arma el propio backend ("La consulta ha sido finalizada
//     exitosamente. El paciente puede proceder a las siguientes
//     indicaciones médicas."). El frontend real IGNORA por completo
//     data.mensaje y siempre muestra el texto fijo "Expediente clínico
//     actualizado exitosamente.", sin importar si se finalizó o no.
//
// Gaps de BACKEND (mensajes que no coinciden con el texto literal del
// documento, aunque la funcionalidad sí ocurre):
//
//  H) FA05: el documento exige el mensaje "No es posible finalizar la
//     consulta sin registrar un diagnóstico. El campo Diagnóstico es
//     obligatorio." El backend real (validarCierre) devuelve un mensaje
//     distinto según qué validación falle -- "Debe completar todos los
//     campos obligatorios para cerrar la consulta." o "El diagnóstico es
//     obligatorio. Debe contener entre 10 y 5000 caracteres." -- ninguno
//     coincide con el texto de FA05.
//  I) FA01 (orden de laboratorio): el documento exige "Orden de laboratorio
//     generada exitosamente. Número de orden: [Número]. Exámenes: [lista].
//     El paciente debe dirigirse al área de laboratorio." El backend real
//     devuelve "Orden de laboratorio generada exitosamente por un monto de
//     Q[monto]. El paciente debe dirigirse a caja para realizar el pago
//     antes de la toma de muestras." -- ni el número de orden ni la lista
//     de exámenes aparecen, y el destino indicado es distinto.
//  J) FA04 (receta): el documento exige "Receta médica generada
//     exitosamente. Medicamentos: [lista]. El paciente puede adquirirlos en
//     la farmacia de la clínica." El backend real devuelve "Receta médica
//     generada exitosamente. El paciente puede pasar a farmacia." -- sin la
//     lista de medicamentos y con otro cierre de frase.
//  K) FA02 (cita de seguimiento): el documento exige "Cita de seguimiento
//     agendada para el [fecha] a las [hora]. Se enviará notificación al
//     paciente." El frontend real arma "...El paciente recibirá un
//     recordatorio." -- distinto cierre de frase.
//
// Lo que SÍ coincide exactamente con el documento (se prueba como camino
// feliz, sin gap): el TTS + mensaje al iniciar consulta (paso 2), el
// mensaje de FA06 (No Asistió) letra por letra, el mensaje de "Atención
// finalizada para cita #[N]." (paso 12), la actualización automática del
// panel cada 30 segundos, y los campos obligatorios de RN-CU08-02/03.

describe('CU-08 - Consulta Médica', () => {
    const medicoId = 50;

    const citaEnEspera = { id: 800, pacienteNombre: 'Ana Martínez', especialidadNombre: 'Medicina General', fechaHora: '2026-08-27T09:00:00', estadoNombre: 'En Espera', emergencia: false };
    const citaEmergencia = { id: 803, pacienteNombre: 'Pedro López', especialidadNombre: 'Medicina General', fechaHora: '2026-08-27T09:10:00', estadoNombre: 'En Espera', emergencia: true };
    const citaEnConsulta = { id: 801, pacienteNombre: 'Luis Gómez', especialidadNombre: 'Medicina General', fechaHora: '2026-08-27T09:30:00', estadoNombre: 'Consulta Médica', emergencia: false };
    const citaEvaluada = { id: 802, pacienteNombre: 'Carla Ruiz', especialidadNombre: 'Medicina General', fechaHora: '2026-08-27T10:00:00', estadoNombre: 'Evaluado', emergencia: false };

    const cie10 = [{ id: 5, codigo: 'J00', nombre: 'Rinofaringitis aguda (resfriado común)' }];
    const medicamentos = [{ id: 20, nombre: 'Paracetamol 500mg', activo: true, precio: 5.0 }];
    const examenes = [{ id: 10, nombre: 'Hemograma completo', activo: true }];

    const panelVacio = { enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [] };

    // OJO: medico_panel.html hace fetch de los catálogos (CIE-10,
    // medicamentos, exámenes) y del panel automáticamente al cargar el
    // script (init()), sin esperar ninguna acción del usuario. Todos esos
    // intercepts deben registrarse ANTES de cy.visit.
    const abrirPanelConDatos = (panel) => {
        cy.intercept('GET', '/api/cie10', cie10).as('cie10');
        cy.intercept('GET', '/api/medicamentos', medicamentos).as('medicamentos');
        cy.intercept('GET', '/api/examenes-laboratorio', examenes).as('examenes');
        cy.intercept('GET', `/api/medico/${medicoId}/panel`, panel).as('panel');
        cy.simularSesion({ rol: 'Médico', nombre: 'Dr. Juan Pérez', uid: medicoId });
        cy.visit('/medico_panel.html');
        cy.wait(['@cie10', '@medicamentos', '@examenes', '@panel']);
    };

    describe('Flujo normal básico (pasos 1-13 del documento)', () => {
        it('NOTA GAP (A): las tres secciones del panel deben titularse igual que el documento', () => {
            abrirPanelConDatos(panelVacio);
            cy.contains('En Espera de Consulta').should('exist');
            cy.contains('En Consulta Médica').should('exist');
            cy.contains('Evaluados — Pendiente de cierre').should('exist');
        });

        it('Paso 1 (comportamiento real): las tres secciones existen y muestran las citas correspondientes', () => {
            abrirPanelConDatos({
                enEsperaDeConsulta: [citaEnEspera],
                enConsultaMedica: [citaEnConsulta],
                evaluadosPendienteCierre: [citaEvaluada],
            });
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).should('exist');
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre).should('exist');
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).should('exist');
        });

        it('Paso 1: el panel se actualiza automáticamente cada 30 segundos', () => {
            cy.clock();
            cy.intercept('GET', '/api/cie10', cie10).as('cie10');
            cy.intercept('GET', '/api/medicamentos', medicamentos).as('medicamentos');
            cy.intercept('GET', '/api/examenes-laboratorio', examenes).as('examenes');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, panelVacio).as('panel');
            cy.simularSesion({ rol: 'Médico', nombre: 'Dr. Juan Pérez', uid: medicoId });
            cy.visit('/medico_panel.html');
            cy.wait('@panel');

            cy.tick(30000);
            cy.get('@panel.all').should('have.length', 2);
        });

        it('NOTA GAP (B): cada tarjeta debe mostrar número de cita, especialidad y estado, además del paciente y la fecha', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEnEspera], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).within(() => {
                cy.contains(String(citaEnEspera.id)).should('exist');
                cy.contains(citaEnEspera.especialidadNombre).should('exist');
                cy.contains(citaEnEspera.estadoNombre).should('exist');
                cy.contains('27').should('exist'); // día de la fecha completa, no solo la hora
            });
        });

        it('NOTA GAP (C): la indicación de prioridad debe decir "Emergencia" (texto exacto del documento)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEmergencia], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.contains('#l-espera li', citaEmergencia.pacienteNombre).contains('Emergencia').should('exist');
        });

        it('Paso 2: al iniciar consulta, el sistema anuncia por voz y transiciona la cita a "Consulta Médica"', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEnEspera], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.window().then((win) => cy.stub(win.speechSynthesis, 'speak').as('speak'));
            cy.intercept('POST', `/api/medico/citas/${citaEnEspera.id}/iniciar-consulta`, {
                id: citaEnEspera.id, pacienteNombre: citaEnEspera.pacienteNombre, estadoNombre: 'Consulta Médica', emergencia: false,
                mensaje: `Turno número ${citaEnEspera.id}. Paciente ${citaEnEspera.pacienteNombre}, favor pasar a consulta médica.`,
            }).as('iniciar');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, panelVacio).as('panelTrasIniciar');

            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).contains('button', 'Iniciar consulta').click();
            cy.wait('@iniciar');
            cy.get('#msg').should('contain.text', `Turno número ${citaEnEspera.id}. Paciente ${citaEnEspera.pacienteNombre}, favor pasar a consulta médica.`);
            cy.get('@speak').should('have.been.calledOnce');
        });

        it('NOTA GAP (D): el botón para completar la consulta debe decir "Ver / Completar Consulta"', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre)
                .contains('button', 'Ver / Completar Consulta')
                .should('exist');
        });

        it('Paso 3 (comportamiento real): "Continuar evaluación" abre el formulario con el número de cita precargado', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre).contains('button', 'Continuar evaluación').click();
            cy.get('#area-consulta').should('be.visible');
            cy.get('#c-id').should('have.text', String(citaEnConsulta.id));
            cy.get('#citaActual').should('have.value', String(citaEnConsulta.id));
        });

        it('Paso 4 [RN-CU08-02]: motivo, hallazgos clínicos y plan de tratamiento son obligatorios; CIE-10 tiene autocompletado', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#mot').should('have.attr', 'required');
            cy.get('#hal').should('have.attr', 'required');
            cy.get('#trat').should('have.attr', 'required');
            cy.get('#cie10-input').should('have.attr', 'list', 'cie10-list');
            cy.get('#cie10-list option').should('have.length', cie10.length);
        });

        it('NOTA GAP (E): el formulario debe incluir un campo "Notas Adicionales"', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.contains('label', 'Notas Adicionales').should('exist');
        });

        it('NOTA GAP (F): el botón para guardar la consulta debe decir "Guardar Consulta"', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#form-consulta').contains('button', 'Guardar Consulta').should('exist');
        });

        it('Pasos 5-9 (comportamiento real): completa y finaliza la consulta con diagnóstico válido', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre, leve rigidez de nuca.');
            cy.get('#diag').type('Cefalea tensional asociada a estrés laboral.');
            cy.get('#trat').type('Analgésico y reposo; reevaluar en una semana.');
            cy.get('#fin').check();

            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`, {
                id: 1, citaId: citaEnConsulta.id, pacienteNombre: citaEnConsulta.pacienteNombre, medicoNombre: 'Dr. Juan Pérez',
                motivoVisita: 'x', hallazgosClinicos: 'x', diagnostico: 'x', planTratamiento: 'x',
                mensaje: 'La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.',
            }).as('guardar');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, { enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] }).as('panelTrasGuardar');

            cy.get('#form-consulta').contains('button', 'Guardar historial clínico').click();
            cy.wait('@guardar').its('request.body').should('deep.include', { finalizar: true });
            cy.get('#area-consulta').should('have.class', 'd-none');
        });

        it('NOTA GAP (G): al finalizar, el mensaje mostrado debe ser el que arma el backend, no un texto fijo genérico', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre, leve rigidez de nuca.');
            cy.get('#diag').type('Cefalea tensional asociada a estrés laboral.');
            cy.get('#trat').type('Analgésico y reposo; reevaluar en una semana.');
            cy.get('#fin').check();

            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`, {
                id: 1, citaId: citaEnConsulta.id, pacienteNombre: citaEnConsulta.pacienteNombre,
                mensaje: 'La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.',
            }).as('guardar');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, panelVacio).as('panelTrasGuardar');

            cy.get('#form-consulta').contains('button', 'Guardar historial clínico').click();
            cy.wait('@guardar');
            // Texto exacto del documento (paso 10). El frontend real muestra
            // "Expediente clínico actualizado exitosamente." sin importar lo
            // que haya devuelto el backend.
            cy.get('#msg').should('have.text', 'La consulta ha sido finalizada exitosamente. El paciente puede proceder a las siguientes indicaciones médicas.');
        });

        it('Paso 12 (comportamiento real): "Finalizar atención" transiciona la cita y muestra el mensaje EXACTO del documento', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] });
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/finalizar-atencion`, {
                id: citaEvaluada.id, pacienteNombre: citaEvaluada.pacienteNombre, estadoNombre: 'Atención Finalizada', emergencia: false,
                mensaje: `Atención finalizada para cita #${citaEvaluada.id}.`,
            }).as('finalizar');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, panelVacio).as('panelTrasFinalizar');

            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Finalizar atención').click();
            cy.wait('@finalizar');
            cy.get('#msg').should('have.text', `Atención finalizada para cita #${citaEvaluada.id}.`);
        });
    });

    describe('FA05 - Intento de finalizar consulta sin diagnóstico', () => {
        it('NOTA GAP (H): el mensaje de rechazo debe ser el texto exacto de FA05', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre.');
            cy.get('#trat').type('Analgésico y reposo.');
            cy.get('#fin').check();
            // Diagnóstico queda vacío a propósito.

            // Mensaje real que el backend (validarCierre) devuelve hoy para
            // este caso -- no el que pide el documento en FA05.
            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`, {
                statusCode: 400,
                body: { message: 'El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.' },
            }).as('rechazo');

            cy.get('#form-consulta').contains('button', 'Guardar historial clínico').click();
            cy.wait('@rechazo');
            // Texto exacto de FA05 en el documento.
            cy.get('#msg').should('have.text', 'No es posible finalizar la consulta sin registrar un diagnóstico. El campo Diagnóstico es obligatorio.');
        });

        it('comportamiento real: el mensaje que realmente devuelve el backend sí se muestra tal cual (aunque no sea el de FA05)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Continuar evaluación').click();
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre.');
            cy.get('#trat').type('Analgésico y reposo.');
            cy.get('#fin').check();

            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`, {
                statusCode: 400,
                body: { message: 'El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.' },
            }).as('rechazo');

            cy.get('#form-consulta').contains('button', 'Guardar historial clínico').click();
            cy.wait('@rechazo');
            cy.get('#msg').should('contain.text', 'El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.');
            cy.get('#area-consulta').should('be.visible');
        });
    });

    describe('FA01 - El paciente requiere exámenes de laboratorio', () => {
        beforeEach(() => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] });
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Orden de laboratorio').click();
            cy.get('#modal-acciones').should('be.visible');
            cy.get('#form-lab').should('be.visible');
        });

        it('Pasos 3-4: el formulario muestra el catálogo de exámenes disponibles (selección múltiple) y permite agregar observaciones', () => {
            cy.get('#l-examenes option').should('have.length', examenes.length);
            cy.get('#l-examenes').should('have.attr', 'multiple');
            cy.get('#l-notas').type('Paciente en ayunas.');
        });

        it('NOTA GAP (I): el mensaje de éxito debe ser el texto exacto del documento, con número de orden y lista de exámenes', () => {
            cy.get('#l-examenes').select([String(examenes[0].id)]);
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/orden-laboratorio`, {
                id: 5, citaId: citaEvaluada.id, pacienteNombre: citaEvaluada.pacienteNombre, medicoNombre: 'Dr. Juan Pérez',
                estado: 'Pendiente', montoTotal: 85, fechaCreacion: '2026-08-27T10:05:00',
                mensaje: 'Orden de laboratorio generada exitosamente por un monto de Q85. El paciente debe dirigirse a caja para realizar el pago antes de la toma de muestras.',
            }).as('generarOrden');
            cy.get('#form-lab').contains('button', 'Generar orden de laboratorio').click();
            cy.wait('@generarOrden');
            cy.get('#msg').should('have.text', `Orden de laboratorio generada exitosamente. Número de orden: 5. Exámenes: ${examenes[0].nombre}. El paciente debe dirigirse al área de laboratorio.`);
        });
    });

    describe('FA04 - El paciente requiere medicamentos', () => {
        beforeEach(() => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] });
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Generar receta').click();
            cy.get('#modal-acciones').should('be.visible');
            cy.get('#form-receta').should('be.visible');
        });

        it('Pasos 3: permite ingresar medicamento, dosis, frecuencia, duración e indicaciones', () => {
            cy.get('#r-med').select(String(medicamentos[0].id));
            cy.get('#r-dosis').type('500mg');
            cy.get('#r-frec').type('Cada 8 horas');
            cy.get('#r-dur').type('5 días');
            cy.get('#r-cant').type('15');
            cy.get('#r-ind').type('Tomar con alimentos.');
            cy.get('#add-med-form').contains('button', 'Añadir medicamento a la receta').click();
            cy.contains('#tb-receta-items tr', medicamentos[0].nombre).should('exist');
        });

        it('NOTA GAP (J): el mensaje de éxito debe ser el texto exacto del documento, con la lista de medicamentos', () => {
            cy.get('#r-med').select(String(medicamentos[0].id));
            cy.get('#r-dosis').type('500mg');
            cy.get('#r-frec').type('Cada 8 horas');
            cy.get('#r-dur').type('5 días');
            cy.get('#r-cant').type('15');
            cy.get('#add-med-form').contains('button', 'Añadir medicamento a la receta').click();

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/receta`, {
                id: 3, citaId: citaEvaluada.id, pacienteNombre: citaEvaluada.pacienteNombre, medicoNombre: 'Dr. Juan Pérez',
                mensaje: 'Receta médica generada exitosamente. El paciente puede pasar a farmacia.',
            }).as('generarReceta');
            cy.contains('button', 'Generar receta médica').click();
            cy.wait('@generarReceta');
            cy.get('#msg').should('have.text', `Receta médica generada exitosamente. Medicamentos: ${medicamentos[0].nombre}. El paciente puede adquirirlos en la farmacia de la clínica.`);
        });
    });

    describe('FA02 - Cita de seguimiento', () => {
        beforeEach(() => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [], evaluadosPendienteCierre: [citaEvaluada] });
            cy.contains('#l-eval li', citaEvaluada.pacienteNombre).contains('button', 'Agendar seguimiento').click();
            cy.get('#modal-acciones').should('be.visible');
            cy.get('#form-seg').should('be.visible');
        });

        it('Paso 2: el formulario de seguimiento pide tipo, prioridad, fecha, hora y motivo', () => {
            cy.get('#s-tipo').should('exist');
            cy.get('#s-prioridad').should('exist');
            cy.get('#s-fecha').should('have.attr', 'required');
            cy.get('#s-hora').should('exist');
            cy.get('#s-motivo').should('have.attr', 'required').and('have.attr', 'minlength', '10');
        });

        it('NOTA GAP (K): el mensaje de confirmación debe ser el texto exacto del documento', () => {
            cy.get('#s-tipo').select('Monitoreo de Tratamiento');
            cy.get('#s-prioridad').select('Media');

            cy.intercept('GET', '/api/citas/horarios-disponibles*', ['2026-09-01T09:00:00']).as('horarios');
            cy.get('#s-fecha').type('2026-09-01');
            cy.wait('@horarios');
            cy.get('#s-hora').select('2026-09-01T09:00:00');
            cy.get('#s-motivo').type('Revisión de evolución del tratamiento.');

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                id: 9, pacienteNombre: citaEvaluada.pacienteNombre, fechaHora: '2026-09-01T09:00:00',
            }).as('agendarSeg');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@agendarSeg');
            // Texto exacto del documento (FA02, paso 4).
            cy.get('#msg').should('contain.text', 'Se enviará notificación al paciente.');
        });
    });

    describe('FA06 - Paciente no asistió', () => {
        it('el mensaje coincide EXACTO con el documento', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEnEspera], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).contains('button', 'No asistió').click();

            cy.get('#app-confirm-modal').should('be.visible');
            cy.get('#app-confirm-titulo').should('have.text', '¿Marcar como no asistió?');
            cy.get('#app-confirm-mensaje').should('have.text', '¿Marcar paciente como no asistió? La cita se cerrará.');

            cy.intercept('POST', `/api/medico/citas/${citaEnEspera.id}/no-asistio`, {
                id: citaEnEspera.id, pacienteNombre: citaEnEspera.pacienteNombre, estadoNombre: 'No Asistió', emergencia: false,
                mensaje: `Cita #${citaEnEspera.id} marcada como No Asistió.`,
            }).as('noAsistio');
            cy.intercept('GET', `/api/medico/${medicoId}/panel`, panelVacio).as('panelTrasNoAsistio');

            cy.get('#app-confirm-aceptar').click();
            cy.wait('@noAsistio');
            // Texto exacto del documento.
            cy.get('#msg').should('have.text', `Cita #${citaEnEspera.id} marcada como No Asistió.`);
        });

        it('cancelar el modal no transiciona la cita', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEnEspera], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).contains('button', 'No asistió').click();
            cy.get('#app-confirm-cancelar').click();
            cy.get('#app-confirm-modal').should('have.class', 'd-none');
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).should('exist');
        });
    });
});