// Ubicación en tu proyecto: qa/cypress/e2e/10_CU09_gestion_laboratorio.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 9_CU_Gestion_de_Laboratorio.docx
// y las reglas de negocio RN-CU09-01 / RN-CU09-02 / RN-GLOBAL-004 / RNF-007 /
// RNF-024 -- afirma lo que el documento exige, no lo que ya había en el
// código. Para que pase en verde necesitás pegar los cambios de:
//   - laboratorio.html
//   - RegistrarResultadoRequestDTO.java
//   - LaboratorioResultadoService.java (interfaz)
//   - LaboratorioResultadoServiceImpl.java
//   - LaboratorioResultadoController.java
//
// Historial de la auditoría (para que quede el rastro):
//
//   RESUELTO -- notificación al médico (RNF-007): el documento decía "no
//   hay notificación automática"; el backend YA envía el correo al publicar
//   (confirmado por el usuario). No es verificable desde este spec de
//   frontend (ocurre server-side), así que no genera ningún test.
//
//   RESUELTO -- "Fecha del Resultado" (paso 9): faltaba el campo, ya se
//   agregó como input datetime-local en laboratorio.html y se manda en el
//   payload de guardar.
//
//   RESUELTO -- rol Supervisor para publicar (RN-CU09-02 / RNF-024): ya
//   estaba bien implementado, se mantiene la cobertura.
//
//   RESUELTO -- reabrir para corrección (RNF-024): no existía ningún flujo
//   de autorización de supervisor para corregir un resultado publicado; se
//   agregó el endpoint /reabrir + botón "Reabrir para corrección" visible
//   solo para Supervisor/Administrador.
//
//   RESUELTO -- nombres de botones: el documento dice "Guardar Resultado" y
//   "Publicar resultado"; el código decía "Guardar borrador" y "Publicar
//   definitivo". Se renombraron para calzar con el texto exacto del CU.
//
//   PENDIENTE DE CONFIRMAR -- RN-CU09-01 (orden Pendiente de pago): el
//   backend rechaza registrarResultado() si orden.estado != EN_PROCESO
//   (ver LaboratorioResultadoServiceImpl), pero el frontend no oculta el
//   formulario para una orden Pendiente -- deja que el usuario intente y
//   recién ahí muestra el error. El spec prueba exactamente ese
//   comportamiento (formulario visible + rechazo del backend al guardar).
//   Si al probar manualmente una orden que TODAVÍA NO se ha pagado en caja
//   (CU-16) el resultado sí se guarda en la BD, sería un bug aparte del
//   check de estado -- avisame para revisarlo con el repositorio real.
//
//   SIN CAMBIOS DE CÓDIGO -- el mensaje "Requiere revisión" (RN-CU09-02)
//   que arma el backend cuando fueraDeRango=true nunca llega a mostrarse al
//   guardar porque save() en el frontend ignora el campo "mensaje" de la
//   respuesta y muestra un texto fijo. Se deja como nota para decidir si se
//   quiere mostrar ese aviso también al guardar (hoy solo se ve el badge
//   visual después de publicar).

describe('CU-09 - Gestión de Laboratorio', () => {

    const ordenPendiente = {
        id: 201, pacienteNombre: 'Carlos Enrique Pérez', medicoNombre: 'Dra. Silvia Morán',
        estado: 'PENDIENTE', esExterna: false, montoTotal: 150.00, notas: '', fechaCreacion: '2026-08-27T08:00:00',
        examenes: [
            { id: 40, examenNombre: 'Examen general de orina', monto: 150.00, valorResultado: null, unidad: null, rangoReferencia: null, notasResultado: null, fueraDeRango: false, publicado: false, fechaResultado: null }
        ]
    };

    const ordenEnProceso = {
        id: 202, pacienteNombre: 'María Fernanda López', medicoNombre: 'Dr. Hugo Castañeda',
        estado: 'EN_PROCESO', esExterna: false, montoTotal: 210.00, notas: 'Paciente en ayunas de 8 horas.', fechaCreacion: '2026-08-27T08:30:00',
        examenes: [
            { id: 41, examenNombre: 'Hemograma completo', monto: 120.00, valorResultado: null, unidad: null, rangoReferencia: null, notasResultado: null, fueraDeRango: false, publicado: false, fechaResultado: null },
            { id: 42, examenNombre: 'Glucosa en ayunas', monto: 90.00, valorResultado: '182', unidad: 'mg/dL', rangoReferencia: '70 - 100 mg/dL', notasResultado: 'Paciente refiere no haber ayunado.', fueraDeRango: true, publicado: true, fechaResultado: '2026-08-27T09:20:00' }
        ]
    };

    const ordenExterna = {
        id: 203, pacienteNombre: 'Ana Lucía Ramírez', medicoNombre: 'Dr. Hugo Castañeda',
        estado: 'COMPLETADA', esExterna: true, montoTotal: 0, notas: 'Orden externa, resultados en seguimiento.', fechaCreacion: '2026-08-20T10:00:00',
        examenes: [
            { id: 43, examenNombre: 'Tomografía abdominal', monto: 0, valorResultado: 'Ver informe físico', unidad: null, rangoReferencia: null, notasResultado: 'Traído por el paciente.', fueraDeRango: false, publicado: true, fechaResultado: '2026-08-20T11:00:00' }
        ]
    };

    // El select de estado no trae "selected" en el HTML -> el navegador toma
    // como valor por defecto la PRIMERA <option>, que es EN_PROCESO. Por eso
    // loadOrd() en la carga inicial siempre pide estado=EN_PROCESO.
    const abrirListado = (rol, ordenes = [ordenEnProceso]) => {
        cy.intercept('GET', '/api/laboratorio/ordenes?estado=EN_PROCESO', ordenes).as('ordenesEnProceso');
        cy.simularSesion({ rol, nombre: rol === 'SupervisorLaboratorio' ? 'Ing. Regina Solís' : 'Téc. Marco Aguilar', uid: 60 });
        cy.visit('/laboratorio.html');
        cy.wait('@ordenesEnProceso');
    };

    const abrirOrden = (rol, orden) => {
        abrirListado(rol, [orden]);
        cy.intercept('GET', `/api/laboratorio/ordenes/${orden.id}`, orden).as('detalleOrden');
        cy.contains('#tb-ord tr', orden.pacienteNombre).contains('button', 'Abrir orden').click();
        cy.wait('@detalleOrden');
    };

    describe('Flujo normal básico (pasos 1-15 del documento)', () => {

        it('Paso 1: la pantalla de "Órdenes de Laboratorio" muestra filtros por estado, paciente y médico', () => {
            abrirListado('Laboratorista');
            cy.get('#filtro-estado').should('exist');
            cy.get('#filtro-paciente').should('exist');
            cy.get('#filtro-medico').should('exist');
            cy.contains('#tb-ord tr', ordenEnProceso.pacienteNombre).should('exist');
        });

        it('Paso 2-3: seleccionar una orden navega al detalle y muestra el resumen completo', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            cy.get('#area-lista').should('have.class', 'd-none');
            cy.get('#area-res').should('not.have.class', 'd-none');
            cy.get('#o-id').should('have.text', String(ordenEnProceso.id));
            cy.get('#o-pac').should('have.text', ordenEnProceso.pacienteNombre);
            cy.get('#o-med').should('have.text', ordenEnProceso.medicoNombre);
            cy.get('#o-tot').should('have.text', String(ordenEnProceso.montoTotal));
        });

        it('Paso 3: debajo del resumen se lista cada examen con su nombre y monto individual', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            ordenEnProceso.examenes.forEach(e => {
                cy.contains('#l-ex .well', e.examenNombre).should('contain.text', `Q${e.monto}`);
            });
        });

        it('Pasos 8-9: el personal completa Valor, Unidad, Rango, Fecha del Resultado y Notas del examen sin resultado', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            const examenSinResultado = ordenEnProceso.examenes[0];
            cy.get(`#v-${examenSinResultado.id}`).type('4.8');
            cy.get(`#u-${examenSinResultado.id}`).type('M/uL');
            cy.get(`#r-${examenSinResultado.id}`).type('4.2 - 5.4 M/uL');
            cy.get(`#fr-${examenSinResultado.id}`).should('exist');
            cy.get(`#n-${examenSinResultado.id}`).type('Sin observaciones.');
        });

        it('Paso 9: el campo "Fecha del Resultado" existe, viene pre-cargado y se manda en el payload de guardar', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            const e = ordenEnProceso.examenes[0];
            cy.contains('label', 'Fecha del resultado').should('exist');
            cy.get(`#fr-${e.id}`).should('exist').invoke('val').should('not.be.empty');
            cy.get(`#v-${e.id}`).type('4.8');

            cy.intercept('POST', `/api/laboratorio/examenes/${e.id}/resultado`, {
                mensaje: 'Resultado guardado exitosamente.'
            }).as('guardar');

            cy.contains('.well', e.examenNombre).contains('button', 'Guardar Resultado').click();
            cy.wait('@guardar').its('request.body').should('have.property', 'fechaResultado');
            cy.get('#msg').should('contain.text', 'Resultado guardado exitosamente.');
        });

        it('Paso 10: al publicar, el sistema pide confirmación antes de ejecutar la acción', () => {
            abrirOrden('SupervisorLaboratorio', ordenEnProceso);
            const e = ordenEnProceso.examenes[0];
            cy.get(`#v-${e.id}`).type('4.8');
            cy.contains('.well', e.examenNombre).contains('button', 'Publicar resultado').click();
            cy.get('#app-confirm-mensaje').should('contain.text', 'Esta acción es irreversible');
            cy.get('#app-confirm-aceptar').should('have.text', 'Publicar');
        });

        it('Pasos 10-12: confirmar la publicación llama al backend y muestra "Resultado publicado exitosamente."', () => {
            abrirOrden('SupervisorLaboratorio', ordenEnProceso);
            const e = ordenEnProceso.examenes[0];
            cy.get(`#v-${e.id}`).type('4.8');
            cy.contains('.well', e.examenNombre).contains('button', 'Publicar resultado').click();

            const ordenTrasPublicar = JSON.parse(JSON.stringify(ordenEnProceso));
            ordenTrasPublicar.examenes[0].publicado = true;

            cy.intercept('POST', `/api/laboratorio/examenes/${e.id}/publicar`, { mensaje: 'Resultado publicado exitosamente.' }).as('publicar');
            cy.intercept('GET', `/api/laboratorio/ordenes/${ordenEnProceso.id}`, ordenTrasPublicar).as('detalleTrasPublicar');

            cy.get('#app-confirm-aceptar').click();
            cy.wait('@publicar');
            cy.get('#msg').should('contain.text', 'Resultado publicado exitosamente.');
        });

        it('Paso 12: una vez publicado el examen, el botón de publicar queda deshabilitado', () => {
            const ordenConPublicado = JSON.parse(JSON.stringify(ordenEnProceso));
            ordenConPublicado.examenes[0].publicado = true;
            ordenConPublicado.examenes[0].valorResultado = '4.8';

            abrirOrden('SupervisorLaboratorio', ordenConPublicado);
            cy.contains('.well', ordenConPublicado.examenes[0].examenNombre)
                .contains('button', 'Publicar resultado')
                .should('be.disabled');
        });

        it('Postcondición: todos los campos de un examen publicado quedan de solo lectura (inmutabilidad, RNF-024)', () => {
            const ordenConPublicado = JSON.parse(JSON.stringify(ordenEnProceso));
            const e = ordenConPublicado.examenes[0];
            e.publicado = true; e.valorResultado = '4.8'; e.unidad = 'M/uL'; e.rangoReferencia = '4.2 - 5.4 M/uL';

            abrirOrden('SupervisorLaboratorio', ordenConPublicado);
            cy.get(`#v-${e.id}`).should('be.disabled');
            cy.get(`#u-${e.id}`).should('be.disabled');
            cy.get(`#r-${e.id}`).should('be.disabled');
            cy.get(`#n-${e.id}`).should('be.disabled');
            cy.get(`#fr-${e.id}`).should('be.disabled');
        });
    });

    describe('RNF-024 - Reabrir un resultado publicado requiere autorización de supervisor', () => {

        const ordenConPublicado = () => {
            const o = JSON.parse(JSON.stringify(ordenEnProceso));
            o.examenes[0].publicado = true;
            o.examenes[0].valorResultado = '4.8';
            return o;
        };

        it('Rol Laboratorista: NO ve el botón de reabrir en un examen publicado', () => {
            abrirOrden('Laboratorista', ordenConPublicado());
            cy.contains('button', 'Reabrir para corrección').should('not.exist');
        });

        it('Rol Supervisor de Laboratorio: SÍ ve el botón de reabrir en un examen publicado', () => {
            abrirOrden('SupervisorLaboratorio', ordenConPublicado());
            cy.contains('button', 'Reabrir para corrección').should('exist');
        });

        it('Reabrir pide confirmación y, al aceptar, vuelve a habilitar los campos del examen', () => {
            const orden = ordenConPublicado();
            abrirOrden('SupervisorLaboratorio', orden);
            const e = orden.examenes[0];

            cy.contains('.well', e.examenNombre).contains('button', 'Reabrir para corrección').click();
            cy.get('#app-confirm-mensaje').should('contain.text', 'Reabrir este resultado');

            const ordenReabierta = JSON.parse(JSON.stringify(orden));
            ordenReabierta.examenes[0].publicado = false;

            cy.intercept('POST', `/api/laboratorio/examenes/${e.id}/reabrir`, { mensaje: 'Resultado reabierto para corrección.' }).as('reabrir');
            cy.intercept('GET', `/api/laboratorio/ordenes/${orden.id}`, ordenReabierta).as('detalleReabierto');

            cy.get('#app-confirm-aceptar').click();
            cy.wait('@reabrir');
            cy.get('#msg').should('contain.text', 'Resultado reabierto para corrección.');
            cy.get(`#v-${e.id}`).should('not.be.disabled');
        });
    });

    describe('RN-CU09-02 / RNF-024 - Solo el Supervisor de Laboratorio puede publicar', () => {

        it('Rol Laboratorista: el botón "Publicar resultado" existe pero está deshabilitado', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            const e = ordenEnProceso.examenes[0];
            cy.contains('.well', e.examenNombre)
                .contains('button', 'Publicar resultado')
                .should('be.disabled')
                .and('have.attr', 'title')
                .and('match', /Supervisor/);
        });

        it('Rol Supervisor de Laboratorio: el botón "Publicar resultado" está habilitado', () => {
            abrirOrden('SupervisorLaboratorio', ordenEnProceso);
            const e = ordenEnProceso.examenes[0];
            cy.get(`#v-${e.id}`).type('4.8');
            cy.contains('.well', e.examenNombre)
                .contains('button', 'Publicar resultado')
                .should('not.be.disabled');
        });
    });

    describe('RN-CU09-01 - El cobro debe completarse antes de la toma de muestras', () => {

        it('Pendiente de confirmar: una orden Pendiente de pago muestra el formulario, y el backend la rechaza al guardar', () => {
            // Ver nota "PENDIENTE DE CONFIRMAR" en la cabecera del archivo.
            // Este test documenta el comportamiento actual (formulario visible
            // + rechazo en el guardar), no necesariamente el deseado.
            abrirOrden('Laboratorista', ordenPendiente);
            const e = ordenPendiente.examenes[0];
            cy.get(`#v-${e.id}`).type('Positivo');

            cy.intercept('POST', `/api/laboratorio/examenes/${e.id}/resultado`, {
                statusCode: 400,
                body: { error: "No es posible registrar resultados: la orden se encuentra en estado 'Pendiente'." }
            }).as('guardarRechazado');

            cy.contains('.well', e.examenNombre).contains('button', 'Guardar Resultado').click();
            cy.wait('@guardarRechazado');
            cy.get('#msg').should('contain.text', "No es posible registrar resultados");
        });
    });

    describe('FA01 - Orden marcada como externa', () => {

        it('La orden externa muestra la etiqueta "Muestra externa" en el detalle', () => {
            abrirOrden('Laboratorista', ordenExterna);
            cy.get('#badge-ext').should('be.visible').and('contain.text', 'Muestra externa');
        });
    });

    describe('FA02 - Resultado marcado fuera de rango', () => {

        it('Un examen publicado y fuera de rango muestra el badge de alerta visual', () => {
            abrirOrden('Laboratorista', ordenEnProceso);
            const eFuera = ordenEnProceso.examenes[1]; // Glucosa en ayunas, fueraDeRango: true, publicado: true
            cy.contains('.well', eFuera.examenNombre).contains('.badge-danger', 'Fuera de rango').should('exist');
        });
    });
});