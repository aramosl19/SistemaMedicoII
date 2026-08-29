// Ubicación en tu proyecto: qa/cypress/e2e/09_CU08_consulta_medica.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 8_CU_Consulta_Medica.docx.
//
// Estado actual (post-fixes de medico_panel.html):
//  A, B, C, D, E, F, G, K(texto) -> RESUELTOS en frontend. Estos tests ahora
//  confirman comportamiento correcto, ya no documentan gaps.
//  I, J -> RESUELTOS en backend (OrdenLaboratorioServiceImpl / RecetaMedicaServiceImpl
//  ya arman el texto exacto). Los mocks de este spec se actualizaron para reflejar
//  la respuesta real del backend corregido.
//  H -> RESUELTO en frontend. #diag ahora se comporta igual que el resto de
//  campos del formulario: al marcar "Finalizar" se vuelve obligatorio
//  (validarCierre()) y reutiliza el minlength/maxlength=10/5000 que ya tenía.
//  PENDIENTE (no resuelto en este spec): RN-CU08-02 ("Debe completar todos los
//  campos obligatorios para cerrar la consulta.") no es alcanzable vía UI mientras
//  #mot, #hal y #trat mantengan required nativo -> el navegador bloquea el submit
//  antes de que el backend pueda devolver ese mensaje genérico. Pendiente decisión.
//
// ACTUALIZACIÓN (Regy): abrirExpediente() (botón "Ver / Completar Consulta") ahora
// hace GET /api/medico/citas/{id}/consulta para recuperar el borrador existente
// (fix del bug donde guardarConsulta con finalizar=false no se recuperaba al
// reabrir el expediente). Sin mockear esa ruta, Cypress la deja pasar al backend
// real -> 401 -> App.apiFetch la trata como sesión expirada y redirige a
// login.html, tumbando cualquier assertion posterior al click. Se agrega el
// intercept en abrirPanelConDatos (comodín /citas/*/consulta) devolviendo null,
// que es lo que responde obtenerBorrador() cuando la cita todavía no tiene
// consulta guardada -- justo el caso de citaEnConsulta en este spec.

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
        // abrirExpediente() (botón "Ver / Completar Consulta") trae el borrador
        // existente vía GET; por defecto no hay consulta previa -> null (200).
        cy.intercept('GET', '/api/medico/citas/*/consulta', { body: null }).as('borrador');
        cy.simularSesion({ rol: 'Médico', nombre: 'Dr. Juan Pérez', uid: medicoId });
        cy.visit('/medico_panel.html');
        cy.wait(['@cie10', '@medicamentos', '@examenes', '@panel']);
    };

    describe('Flujo normal básico (pasos 1-13 del documento)', () => {
        it('Título de las tres secciones del panel (antes GAP A, ya resuelto)', () => {
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

        it('Tarjeta muestra número de cita, especialidad, estado y fecha completa (antes GAP B, ya resuelto)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [citaEnEspera], enConsultaMedica: [], evaluadosPendienteCierre: [] });
            cy.contains('#l-espera li', citaEnEspera.pacienteNombre).within(() => {
                cy.contains(String(citaEnEspera.id)).should('exist');
                cy.contains(citaEnEspera.especialidadNombre).should('exist');
                cy.contains(citaEnEspera.estadoNombre).should('exist');
                cy.contains('27').should('exist'); // día de la fecha completa, no solo la hora
            });
        });

        it('Indicación de prioridad dice "Emergencia" (antes GAP C, ya resuelto)', () => {
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

        it('Botón para completar la consulta dice "Ver / Completar Consulta" (antes GAP D, ya resuelto)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre)
                .contains('button', 'Ver / Completar Consulta')
                .should('exist');
        });

        it('Paso 3 (comportamiento real): "Ver / Completar Consulta" abre el formulario con el número de cita precargado', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre).contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#area-consulta').should('be.visible');
            cy.get('#c-id').should('have.text', String(citaEnConsulta.id));
            cy.get('#citaActual').should('have.value', String(citaEnConsulta.id));
        });

        it('Paso 3: el formulario también precarga el nombre del paciente', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('#l-consulta li', citaEnConsulta.pacienteNombre).contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#area-consulta').should('contain.text', citaEnConsulta.pacienteNombre);
        });

        it('Paso 4 [RN-CU08-02]: motivo, hallazgos clínicos y plan de tratamiento son obligatorios; CIE-10 tiene autocompletado', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#mot').should('have.attr', 'required');
            cy.get('#hal').should('have.attr', 'required');
            cy.get('#trat').should('have.attr', 'required');
            cy.get('#cie10-input').should('have.attr', 'list', 'cie10-list');
            cy.get('#cie10-list option').should('have.length', cie10.length);
        });

        it('El formulario incluye un campo "Notas Adicionales" (antes GAP E, ya resuelto)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.contains('label', 'Notas Adicionales').should('exist');
        });

        it('Botón para guardar la consulta dice "Guardar Consulta" (antes GAP F, ya resuelto)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#form-consulta').contains('button', 'Guardar Consulta').should('exist');
        });

        it('Pasos 5-9 (comportamiento real): completa y finaliza la consulta con diagnóstico válido', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
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

            cy.get('#form-consulta').contains('button', 'Guardar Consulta').click();
            cy.wait('@guardar').its('request.body').should('deep.include', { finalizar: true });
            cy.get('#area-consulta').should('have.class', 'd-none');
        });

        it('Al finalizar, el mensaje mostrado es el que arma el backend (antes GAP G, ya resuelto)', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
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

            cy.get('#form-consulta').contains('button', 'Guardar Consulta').click();
            cy.wait('@guardar');
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

    describe('FA05 / RN-CU08-01 - Diagnóstico obligatorio y con longitud válida (Backend)', () => {

        it('diagnóstico vacío con cierre marcado: el frontend bloquea el envío y marca el campo en rojo', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre.');
            cy.get('#trat').type('Analgésico y reposo.');
            cy.get('#fin').check();

            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`).as('postConsulta');

            cy.get('#form-consulta').contains('button', 'Guardar Consulta').click();

            cy.get('@postConsulta.all').should('have.length', 0);
            cy.get('#diag').should('have.class', 'is-invalid');
            // FIX SELECTOR: usamos .next() en lugar de + para evitar fallos por nodos de texto ocultos
            cy.get('#diag').next('.invalid-feedback').should('have.text', 'El diagnóstico es obligatorio para finalizar la consulta.');
        });

        it('diagnóstico demasiado corto (<10 caracteres): el backend lo rechaza y el frontend pinta el error bajo el campo', () => {
            abrirPanelConDatos({ enEsperaDeConsulta: [], enConsultaMedica: [citaEnConsulta], evaluadosPendienteCierre: [] });
            cy.contains('button', 'Ver / Completar Consulta').click();
            cy.wait('@borrador');
            cy.get('#mot').type('Dolor de cabeza recurrente desde hace tres días.');
            cy.get('#hal').type('Presión arterial normal, sin fiebre.');
            cy.get('#trat').type('Analgésico y reposo.');

            cy.get('#diag').type('Cefalea.');
            cy.get('#fin').check();

            const mensajeBackend = 'El diagnóstico es obligatorio. Debe contener entre 10 y 5000 caracteres.';

            cy.intercept('POST', `/api/medico/citas/${citaEnConsulta.id}/consulta`, {
                statusCode: 400,
                body: { message: mensajeBackend }
            }).as('postConsulta');

            cy.get('#form-consulta').contains('button', 'Guardar Consulta').click();

            cy.wait('@postConsulta');

            cy.get('#diag').should('have.class', 'is-invalid');
            // FIX SELECTOR: usamos .next() en lugar de +
            cy.get('#diag').next('.invalid-feedback').should('have.text', mensajeBackend);
            cy.get('#msg').should('contain.text', mensajeBackend);
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

        it('El mensaje de éxito es el texto exacto del documento (antes GAP I, ya resuelto en backend)', () => {
            cy.get('#l-examenes').select([String(examenes[0].id)]);
            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/orden-laboratorio`, {
                id: 5, citaId: citaEvaluada.id, pacienteNombre: citaEvaluada.pacienteNombre, medicoNombre: 'Dr. Juan Pérez',
                estado: 'Pendiente', montoTotal: 85, fechaCreacion: '2026-08-27T10:05:00',
                examenes: [{ id: 1, examenNombre: examenes[0].nombre, monto: 85, publicado: false }],
                mensaje: `Orden de laboratorio generada exitosamente. Número de orden: 5. Exámenes: ${examenes[0].nombre}. El paciente debe dirigirse al área de laboratorio.`,
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

        it('El mensaje de éxito es el texto exacto del documento (antes GAP J, ya resuelto en backend)', () => {
            cy.get('#r-med').select(String(medicamentos[0].id));
            cy.get('#r-dosis').type('500mg');
            cy.get('#r-frec').type('Cada 8 horas');
            cy.get('#r-dur').type('5 días');
            cy.get('#r-cant').type('15');
            cy.get('#add-med-form').contains('button', 'Añadir medicamento a la receta').click();

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/receta`, {
                id: 3, citaId: citaEvaluada.id, pacienteNombre: citaEvaluada.pacienteNombre, medicoNombre: 'Dr. Juan Pérez',
                mensaje: `Receta médica generada exitosamente. Medicamentos: ${medicamentos[0].nombre}. El paciente puede adquirirlos en la farmacia de la clínica.`,
            }).as('generarReceta');
            cy.contains('button', 'Generar receta médica').click();
            cy.wait('@generarReceta');
            cy.get('#msg').should('have.text', `Receta médica generada exitosamente. Medicamentos: ${medicamentos[0].nombre}. El paciente puede adquirirlos en la farmacia de la clínica.`);
        });

        it('RN-CU08-03: dosis, frecuencia y duración son obligatorias en el formulario de receta', () => {
            cy.get('#r-med').should('exist');
            cy.get('#r-dosis').should('have.attr', 'required');
            cy.get('#r-frec').should('have.attr', 'required');
            cy.get('#r-dur').should('have.attr', 'required');
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
            cy.get('#s-motivo').should('have.attr', 'required');
            cy.get('#s-motivo').should('have.attr', 'minlength', '10');
        });

        it('El mensaje de confirmación es el texto exacto del documento (antes GAP K, ya resuelto)', () => {
            cy.get('#s-tipo').select('Monitoreo de Tratamiento');
            cy.get('#s-prioridad').select('Media');

            cy.intercept('GET', '/api/citas/horarios-disponibles*', ['2026-09-01T09:00:00']).as('horarios');
            // input[type=date] no dispara 'change' de forma confiable con .type();
            // se fuerza el valor y se dispara el evento manualmente.
            cy.get('#s-fecha').invoke('val', '2026-09-01').trigger('change');
            cy.wait('@horarios');
            cy.get('#s-hora').select('2026-09-01T09:00:00');
            cy.get('#s-motivo').type('Revisión de evolución del tratamiento.');

            cy.intercept('POST', `/api/medico/citas/${citaEvaluada.id}/seguimiento`, {
                id: 9, pacienteNombre: citaEvaluada.pacienteNombre, fechaHora: '2026-09-01T09:00:00',
            }).as('agendarSeg');
            cy.get('#form-seg').contains('button', 'Agendar Cita').click();
            cy.wait('@agendarSeg');
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