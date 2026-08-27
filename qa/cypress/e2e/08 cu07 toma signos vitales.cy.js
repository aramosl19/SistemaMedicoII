// Ubicación en tu proyecto: qa/cypress/e2e/08_CU07_toma_signos_vitales.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 7_CU_Toma_de_Signos_Vitales.docx.
//
// ACTUALIZACIÓN 2: el gap B (acción separada "Registrar Signos Vitales" en
// la tarjeta, paso 2 del documento) se implementó y luego se REVIRTIÓ a
// propósito — en la práctica, un solo clic en "Llamar y Tomar Signos" ya
// llama al paciente Y abre el formulario con el contexto precargado, sin
// pedir un segundo clic. Esta versión del spec queda alineada a ese
// comportamiento de un solo clic (más simple para el uso real).
//
// Resumen de lo que quedó tal cual el documento vs. decisiones explícitas:
//  A) Botón para llamar: "Llamar paciente" -> "Llamar y Tomar Signos"
//     (texto exacto del documento, paso 1).
//  B) REVERTIDO: un solo clic llama Y abre el formulario (no dos acciones
//     separadas como pedía el documento literal en los pasos 1 y 2).
//  C) Botón de envío del formulario: "Registrar en expediente" ->
//     "Registrar Signos Vitales" (texto exacto del documento, paso 9).
//  D) El checkbox de emergencia se mantiene como checkbox simple con label
//     "Emergencia" (decisión explícita, en vez del wording "¿Emergencia?"
//     del documento).
//  E) El mensaje de éxito (rama no-emergencia) es el texto exacto del
//     documento: "Signos vitales del paciente [Nombre] registrados
//     correctamente. El paciente puede regresar a la sala de espera."
//
// NOTA (no es un gap probado aquí): el documento (FA01, paso 6) dice que el
// paciente de emergencia "pasa directamente a consulta médica sin regresar
// a la sala de espera". El backend transiciona la cita al MISMO estado "En
// Espera" sea o no emergencia — la única diferencia real es el orden de
// prioridad en la cola del médico. No es observable desde enfermeria.html,
// así que no se prueba en este spec; sería un test de backend o de
// medico_panel.html.
//
// NOTA (tecnología, no gap): el documento referencia un componente React
// "VitalSignAlertsDisplay" y un hook "useVitalSignAlerts" (RN-CU07-06). La
// app real no usa React — implementa el mismo panel de alertas en tiempo
// real con JS vanilla (#alertas-rt / validarAlertasRT()). Este spec prueba
// el comportamiento funcional descrito por el documento, no la tecnología
// mencionada en él.

describe('CU-07 - Toma de Signos Vitales', () => {
    const pacientePresente = { id: 700, pacienteNombre: 'María García', estadoNombre: 'Paciente Presente', emergencia: false };
    const pacienteEnTriage = { id: 701, pacienteNombre: 'Carlos Pérez', estadoNombre: 'Signos Vitales', emergencia: false };
    const pacienteEmergencia = { id: 702, pacienteNombre: 'Sofía Ruiz', estadoNombre: 'Paciente Presente', emergencia: true };

    // OJO: enfermeria.html llama a cargarEnEspera() automáticamente apenas
    // carga el script, sin esperar ninguna acción del usuario. Por eso el
    // intercept de esa lista SIEMPRE debe registrarse ANTES de cy.visit.
    const abrirPanelConLista = (lista) => {
        cy.intercept('GET', '/api/enfermeria/citas/en-espera', lista).as('listar');
        cy.simularSesion({ rol: 'Enfermero', nombre: 'Lucía Enfermera' });
        cy.visit('/enfermeria.html');
        cy.wait('@listar');
    };

    // Un solo clic: llama al paciente 700 y el formulario se abre directo,
    // con el contexto ya precargado (comportamiento real tras revertir B).
    const llamarYAbrirFormulario = () => {
        abrirPanelConLista([pacientePresente]);
        cy.intercept('POST', '/api/enfermeria/citas/700/llamar', {
            id: 700,
            pacienteNombre: pacientePresente.pacienteNombre,
            estadoNombre: 'Signos Vitales',
            emergencia: false,
            mensaje: 'Turno número 700. Paciente María García, favor pasar a toma de signos vitales.',
        }).as('llamar');
        // cargarEnEspera() se vuelve a ejecutar sola después de llamar() —
        // hay que reinterceptar la MISMA ruta para que esa segunda llamada
        // no le pegue al backend real.
        cy.intercept('GET', '/api/enfermeria/citas/en-espera', [
            { ...pacientePresente, estadoNombre: 'Signos Vitales' },
        ]).as('listarTrasLlamar');

        cy.contains('button', 'Llamar y Tomar Signos').click();
        cy.wait('@llamar');
        cy.wait('@listarTrasLlamar');
        cy.get('#form-area').should('be.visible');
    };

    describe('Flujo normal básico (pasos 1-14 del documento)', () => {
        it('Paso 1: separa a los pacientes en estado "Paciente Presente" de los que ya están en proceso de toma de signos', () => {
            abrirPanelConLista([pacientePresente, pacienteEnTriage]);

            cy.contains('#tb-espera tr', pacientePresente.pacienteNombre).within(() => {
                cy.get('td').eq(1).should('have.text', 'Paciente Presente');
            });
            cy.contains('#tb-espera tr', pacienteEnTriage.pacienteNombre).within(() => {
                cy.contains('.badge', 'En proceso (Triage)').should('exist');
            });
        });

        it('Paso 1: el botón para llamar al paciente dice "Llamar y Tomar Signos" (texto exacto del documento)', () => {
            abrirPanelConLista([pacientePresente]);
            cy.contains('#tb-espera tr', pacientePresente.pacienteNombre)
                .contains('button', 'Llamar y Tomar Signos')
                .should('exist');
        });

        it('Pasos 1-2 (comportamiento real, un solo clic): al llamar, el sistema anuncia por voz, transiciona la cita y abre el formulario ya precargado', () => {
            abrirPanelConLista([pacientePresente]);
            cy.window().then((win) => {
                cy.stub(win.speechSynthesis, 'speak').as('speak');
            });
            cy.intercept('POST', '/api/enfermeria/citas/700/llamar', {
                id: 700,
                pacienteNombre: pacientePresente.pacienteNombre,
                estadoNombre: 'Signos Vitales',
                emergencia: false,
                mensaje: 'Turno número 700. Paciente María García, favor pasar a toma de signos vitales.',
            }).as('llamar');
            cy.intercept('GET', '/api/enfermeria/citas/en-espera', [
                { ...pacientePresente, estadoNombre: 'Signos Vitales' },
            ]).as('listarTrasLlamar');

            cy.contains('button', 'Llamar y Tomar Signos').click();
            cy.wait('@llamar');

            cy.get('#msg').should('contain.text', 'Turno número 700. Paciente María García, favor pasar a toma de signos vitales.');
            cy.get('@speak').should('have.been.calledOnce');
            // Un solo clic ya deja el formulario abierto -- no hace falta
            // una segunda acción (ver NOTA de reversión del gap B arriba).
            cy.get('#form-area').should('be.visible');
        });

        it('Paso 3: el encabezado precarga nombre del paciente y número de cita, y los IDs no son visibles ni editables', () => {
            llamarYAbrirFormulario();

            cy.get('#nom-pac').should('have.text', pacientePresente.pacienteNombre);
            cy.get('#num-cita-text').should('have.text', '700');
            cy.get('#citaId').should('have.attr', 'type', 'hidden').and('have.value', '700');
            cy.get('#form-signos').find('#enfermeroId').should('not.exist');
        });

        it('Paso 4 [RN-CU07-01]: presión sistólica (60-250) y diastólica (40-150) en campos separados', () => {
            llamarYAbrirFormulario();
            cy.get('#s').should('have.attr', 'min', '60').and('have.attr', 'max', '250');
            cy.get('#d').should('have.attr', 'min', '40').and('have.attr', 'max', '150');
        });

        it('Paso 5 [RN-CU07-02]: temperatura entre 34 y 42 °C', () => {
            llamarYAbrirFormulario();
            cy.get('#t').should('have.attr', 'min', '34').and('have.attr', 'max', '42');
        });

        it('Paso 6 [RN-CU07-03]: peso entre 0.5 y 300 kg', () => {
            llamarYAbrirFormulario();
            cy.get('#p').should('have.attr', 'min', '0.5').and('have.attr', 'max', '300');
        });

        it('Paso 7 [RN-CU07-04]: talla entre 30 y 250 cm', () => {
            llamarYAbrirFormulario();
            cy.get('#h').should('have.attr', 'min', '30').and('have.attr', 'max', '250');
        });

        it('Paso 8 [RN-CU07-05]: frecuencia cardíaca entre 30 y 220 lpm', () => {
            llamarYAbrirFormulario();
            cy.get('#fc').should('have.attr', 'min', '30').and('have.attr', 'max', '220');
        });

        it('Paso 9: el botón para registrar los signos vitales dice "Registrar Signos Vitales" (texto exacto del documento)', () => {
            llamarYAbrirFormulario();
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').should('exist');
        });

        it('Pasos 9-11: con todos los valores dentro de rango, registra los signos vitales', () => {
            llamarYAbrirFormulario();
            cy.get('#s').type('120');
            cy.get('#d').type('80');
            cy.get('#t').type('37.0');
            cy.get('#fc').type('75');
            cy.get('#p').type('70.5');
            cy.get('#h').type('175');

            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                id: 1, citaId: 700, pacienteNombre: pacientePresente.pacienteNombre,
                presionSistolica: 120, presionDiastolica: 80, temperatura: 37.0,
                peso: 70.5, talla: 175, frecuenciaCardiaca: 75,
                alertaPresion: false, alertaTemperatura: false, alertaFrecuencia: false,
                emergencia: false, fechaRegistro: '2026-08-25T10:00:00',
                mensaje: 'Signos vitales del paciente María García registrados correctamente. El paciente puede regresar a la sala de espera.',
            }).as('registrar');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@registrar').its('request.body').should('deep.include', {
                presionSistolica: '120', presionDiastolica: '80', temperatura: '37.0',
                peso: '70.5', talla: '175', frecuenciaCardiaca: '75', emergencia: false,
            });
            cy.get('#form-area').should('have.class', 'd-none');
        });

        it('Paso 12: el mensaje de éxito es el texto exacto del documento, incluyendo el regreso a la sala de espera', () => {
            llamarYAbrirFormulario();
            cy.get('#s').type('120'); cy.get('#d').type('80'); cy.get('#t').type('37.0');
            cy.get('#fc').type('75'); cy.get('#p').type('70.5'); cy.get('#h').type('175');

            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                id: 1, citaId: 700, pacienteNombre: pacientePresente.pacienteNombre,
                presionSistolica: 120, presionDiastolica: 80, temperatura: 37.0,
                peso: 70.5, talla: 175, frecuenciaCardiaca: 75,
                alertaPresion: false, alertaTemperatura: false, alertaFrecuencia: false,
                emergencia: false, fechaRegistro: '2026-08-25T10:00:00',
                mensaje: 'Signos vitales del paciente María García registrados correctamente. El paciente puede regresar a la sala de espera.',
            }).as('registrar');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@registrar');

            cy.get('#msg').should('have.text', 'Signos vitales del paciente María García registrados correctamente. El paciente puede regresar a la sala de espera.');
        });
    });

    describe('FA01 - Paciente de emergencia (prioridad)', () => {
        beforeEach(() => {
            abrirPanelConLista([pacienteEmergencia]);
            cy.intercept('POST', '/api/enfermeria/citas/702/llamar', {
                id: 702, pacienteNombre: pacienteEmergencia.pacienteNombre, estadoNombre: 'Signos Vitales',
                emergencia: true, mensaje: 'Turno número 702. Paciente Sofía Ruiz, favor pasar a toma de signos vitales.',
            }).as('llamar');
            cy.intercept('GET', '/api/enfermeria/citas/en-espera', [
                { ...pacienteEmergencia, estadoNombre: 'Signos Vitales' },
            ]).as('listarTrasLlamar');

            cy.contains('button', 'Llamar y Tomar Signos').click();
            cy.wait('@llamar');
            cy.wait('@listarTrasLlamar');
            cy.get('#form-area').should('be.visible');
        });

        it('el indicador de emergencia es un checkbox con label "Emergencia"', () => {
            cy.contains('label', 'Emergencia').find('input[type=checkbox]#em').should('exist');
        });

        it('Pasos 3-5: al marcar la emergencia y registrar, el mensaje coincide EXACTO con el documento', () => {
            cy.get('#em').check();
            cy.get('#s').type('120'); cy.get('#d').type('80'); cy.get('#t').type('37.0');
            cy.get('#fc').type('75'); cy.get('#p').type('70.5'); cy.get('#h').type('175');

            cy.intercept('POST', '/api/enfermeria/citas/702/signos-vitales', {
                id: 2, citaId: 702, pacienteNombre: pacienteEmergencia.pacienteNombre,
                presionSistolica: 120, presionDiastolica: 80, temperatura: 37.0,
                peso: 70.5, talla: 175, frecuenciaCardiaca: 75,
                alertaPresion: false, alertaTemperatura: false, alertaFrecuencia: false,
                emergencia: true, fechaRegistro: '2026-08-25T10:05:00',
                mensaje: 'Signos vitales de emergencia registrados para paciente Sofía Ruiz. El paciente debe pasar directamente a consulta médica.',
            }).as('registrarEmergencia');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@registrarEmergencia').its('request.body').should('include', { emergencia: true });

            cy.get('#msg').should('contain.text', 'Signos vitales de emergencia registrados para paciente Sofía Ruiz. El paciente debe pasar directamente a consulta médica.');
        });
    });

    describe('FA02 - Valores fuera de rango de captura', () => {
        beforeEach(() => {
            llamarYAbrirFormulario();
        });

        it('Pasos 1-2: la temperatura fuera del rango de captura muestra el mensaje específico del campo', () => {
            cy.get('#s').type('120'); cy.get('#d').type('80'); cy.get('#t').type('45');
            cy.get('#fc').type('75'); cy.get('#p').type('70.5'); cy.get('#h').type('175');

            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                statusCode: 400,
                body: { message: 'La temperatura debe estar entre 34.0 y 42.0°C con un decimal.' },
            }).as('errorTemp');
            // El input #t tiene min/max nativos idénticos al rango que
            // valida el backend (34-42) — sin desactivar la validación
            // HTML5 del formulario, el navegador bloquea el submit él solo
            // y el JS/fetch nunca llega a ejecutarse.
            cy.get('#form-signos').invoke('attr', 'novalidate', 'novalidate');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@errorTemp');

            cy.get('#msg').should('contain.text', 'La temperatura debe estar entre 34.0 y 42.0°C con un decimal.');
            cy.get('#form-area').should('be.visible');
        });

        it('Pasos 3-4: tras corregir el valor, se continúa en el paso 9 y el registro se completa', () => {
            cy.get('#s').type('120'); cy.get('#d').type('80'); cy.get('#t').type('45');
            cy.get('#fc').type('75'); cy.get('#p').type('70.5'); cy.get('#h').type('175');
            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                statusCode: 400,
                body: { message: 'La temperatura debe estar entre 34.0 y 42.0°C con un decimal.' },
            }).as('errorTemp');
            cy.get('#form-signos').invoke('attr', 'novalidate', 'novalidate');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@errorTemp');

            cy.get('#t').clear().type('37.0');
            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                id: 3, citaId: 700, pacienteNombre: pacientePresente.pacienteNombre,
                presionSistolica: 120, presionDiastolica: 80, temperatura: 37.0,
                peso: 70.5, talla: 175, frecuenciaCardiaca: 75,
                alertaPresion: false, alertaTemperatura: false, alertaFrecuencia: false,
                emergencia: false, fechaRegistro: '2026-08-25T10:10:00', mensaje: 'ok',
            }).as('registrarOk');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@registrarOk');
            cy.get('#form-area').should('have.class', 'd-none');
        });
    });

    describe('FA03 - Valores fuera de rango clínico normal (alertas en tiempo real)', () => {
        beforeEach(() => {
            llamarYAbrirFormulario();
        });

        it('Pasos 1-2: muestra una alerta visual en tiempo real cuando la presión está fuera del rango clínico normal', () => {
            cy.get('#alertas-rt').should('have.class', 'd-none');
            cy.get('#s').type('180');
            cy.get('#d').type('110');
            cy.get('#alertas-rt').should('be.visible');
            cy.get('#lista-alertas-rt').should('contain.text', 'Presión arterial fuera de rango normal (90/60 - 140/90).');
        });

        it('la alerta desaparece cuando el valor vuelve al rango clínico normal', () => {
            cy.get('#t').type('39.5');
            cy.get('#alertas-rt').should('be.visible');
            cy.get('#t').clear().type('37.0');
            cy.get('#alertas-rt').should('have.class', 'd-none');
        });

        it('Paso 4: al registrar con valores clínicos fuera de rango normal, el sistema conserva la alerta en el resultado', () => {
            cy.get('#s').type('180'); cy.get('#d').type('110'); cy.get('#t').type('37.0');
            cy.get('#fc').type('75'); cy.get('#p').type('70.5'); cy.get('#h').type('175');

            cy.intercept('POST', '/api/enfermeria/citas/700/signos-vitales', {
                id: 4, citaId: 700, pacienteNombre: pacientePresente.pacienteNombre,
                presionSistolica: 180, presionDiastolica: 110, temperatura: 37.0,
                peso: 70.5, talla: 175, frecuenciaCardiaca: 75,
                alertaPresion: true, alertaTemperatura: false, alertaFrecuencia: false,
                emergencia: false, fechaRegistro: '2026-08-25T10:15:00',
                mensaje: 'Signos vitales del paciente María García registrados correctamente. El paciente puede regresar a la sala de espera.',
            }).as('registrarConAlerta');
            cy.get('#form-signos').contains('button', 'Registrar Signos Vitales').click();
            cy.wait('@registrarConAlerta');

            cy.get('#msg').should('contain.text', 'Atención: valores clínicos fuera de rango normal. Se notificará al médico');
        });
    });
});