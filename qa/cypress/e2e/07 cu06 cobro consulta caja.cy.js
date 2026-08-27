// Ubicación en tu proyecto: qa/cypress/e2e/07_CU06_cobro_consulta_caja.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 6_CU_Cobro_de_Consulta_en_Caja.docx.
// Donde caja.html real se desvía del texto/las reglas del documento, la prueba
// queda tal cual el documento lo exige y por lo tanto VA A FALLAR — con un
// comentario "NOTA GAP" explicando la diferencia encontrada. No se ajustó
// ningún selector, texto esperado ni mock para maquillar el resultado.
//
// Gaps de FRONTEND encontrados entre el documento y caja.html:
//
//  A) Doc (paso 2): el detalle de la cuenta debe mostrar especialidad,
//     médico, fecha, hora y monto total. El HTML real (tabla de resultados
//     y área de cobro) SOLO muestra paciente, tipo de servicio, monto e
//     ID — no hay ningún elemento para especialidad, médico, fecha u hora
//     en ninguna de las dos pantallas.
//  B) Doc (paso 6): el campo se llama "Monto Recibido". El HTML real usa
//     la etiqueta "Monto entregado por paciente (Q)".
//  C) Doc (pasos 6 y FA01-4): el botón debe decir "Registrar Pago". El
//     botón real del formulario dice "Registrar ingreso de caja".
//  D) Doc (paso 6): el mensaje de monto insuficiente es "El monto recibido
//     (Q[recibido]) es menor al monto a cobrar (Q[total])" — SIN punto
//     final. El código real agrega un punto final.
//  E) Doc (paso 7): al calcular el cambio se debe mostrar "Monto recibido:
//     Q[monto]. Cambio a devolver: Q[cambio]." El código real
//     (calcularCambioVivo) omite por completo la primera oración y solo
//     muestra "Cambio a devolver: Q[cambio]".
//  F) Doc (paso 10): el botón debe decir "Nuevo Cobro". El botón real dice
//     "Nuevo cobro" (minúscula).
//
// NOTA sobre el mensaje de éxito (paso 8): el documento exige el texto
// "¡Pago registrado exitosamente! Paciente: [Nombre]. La cita ha sido
// actualizada a estado Confirmada.", pero el frontend real solo hace
// App.showAlert('msg', data.mensaje) — muestra literalmente lo que
// devuelva el backend, sin componer el texto en el cliente. Por eso el
// test de ese paso es un test de CONTRATO (se mockea que el backend YA
// manda el texto exacto del documento) y no una prueba de que el backend
// real hoy lo devuelve así. Lo mismo aplica al mensaje de rechazo de
// tarjeta de FA04.
//
// NOTA sobre "tipoServ = LAB" (exámenes de laboratorio): caja.html
// generaliza la pantalla para cobrar también órdenes de laboratorio,
// algo que el documento de CU-06 (específico de consulta médica) no
// contempla. Es una extensión intencional, no una desviación — este
// spec prueba únicamente el camino de "Consulta médica" (tipoServ=CITA).
//
// NOTA sobre la postcondición "la cita se actualiza a estado Confirmada"
// y sobre la cancelación automática a los 10 minutos (FA03, paso 4): son
// comportamientos de backend/job programado que no se reflejan en ningún
// elemento visible de caja.html, así que no se pueden probar desde este
// spec de UI. Deben cubrirse en un test de servicio (backend) aparte.

describe('CU-06 - Cobro de Consulta en Caja', () => {
    const citaPendiente = { id: 900, pacienteNombre: 'Ana López', monto: 150 };

    const abrirPantallaDeCaja = () => {
        cy.simularSesion({ rol: 'Cajero', nombre: 'Carlos Ramírez' });
        cy.visit('/caja.html');
    };

    const buscarPorNumeroCita = () => {
        cy.intercept('GET', '/api/caja/citas/buscar*', [citaPendiente]).as('buscar');
        cy.get('#tipoServ').select('Consulta médica');
        cy.get('#refId').type(String(citaPendiente.id));
        cy.contains('button', 'Buscar cuentas').click();
        cy.wait('@buscar');
    };

    const irAPantallaDeCobro = () => {
        buscarPorNumeroCita();
        cy.contains('tr', citaPendiente.pacienteNombre).contains('button', 'Cobrar').click();
        cy.get('#cobro-area').should('be.visible');
    };

    describe('Flujo normal básico (pasos 1-11 del documento)', () => {
        beforeEach(abrirPantallaDeCaja);

        it('Paso 1 [RN-CU06-01]: busca la cita por número de cita', () => {
            buscarPorNumeroCita();
            cy.get('#tb-cuentas-container').should('be.visible');
            cy.contains('#tb-cuentas tr', citaPendiente.pacienteNombre).should('exist');
        });

        it('Paso 1 [RN-CU06-01]: busca la cita por DPI del paciente', () => {
            cy.intercept('GET', '/api/caja/citas/buscar*', [citaPendiente]).as('buscar');
            cy.get('#tipoServ').select('Consulta médica');
            cy.get('#refDpi').type('1234567890123');
            cy.contains('button', 'Buscar cuentas').click();
            cy.wait('@buscar');
            cy.contains('#tb-cuentas tr', citaPendiente.pacienteNombre).should('exist');
        });

        it('NOTA GAP (A): el detalle de la cuenta debe mostrar especialidad, médico, fecha y hora, además del monto', () => {
            irAPantallaDeCobro();
            // El documento (paso 2) exige estos 4 datos junto con el monto total.
            // No existe NINGÚN elemento para ellos en caja.html — falla contra
            // la app real, que solo muestra paciente, tipo, ID y monto.
            // TODO: confirmar selectores contra caja.html una vez se agreguen.
            cy.get('#c-especialidad').should('not.be.empty');
            cy.get('#c-medico').should('not.be.empty');
            cy.get('#c-fecha').should('not.be.empty');
            cy.get('#c-hora').should('not.be.empty');
        });

        it('Paso 2: el detalle de la cuenta sí muestra el monto total a cobrar', () => {
            irAPantallaDeCobro();
            cy.get('#c-monto').should('contain.text', String(citaPendiente.monto));
        });

        it('Paso 4: las opciones de método de pago son Efectivo, Visa, Mastercard o Débito', () => {
            irAPantallaDeCobro();
            cy.get('#metodo option').then((opts) => {
                const textos = [...opts].map((o) => o.text.trim());
                expect(textos).to.include.members([
                    'Seleccione una opción',
                    'Efectivo (moneda local)',
                    'Tarjeta de crédito Visa',
                    'Tarjeta de crédito Mastercard',
                    'Tarjeta de débito',
                ]);
            });
        });

        it('NOTA GAP (B): el campo de monto recibido debe llamarse "Monto Recibido"', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            // Texto exacto pedido por el documento (paso 6). La app real dice
            // "Monto entregado por paciente (Q)".
            cy.get('#div-efectivo label').should('have.text', 'Monto Recibido');
        });

        it('NOTA GAP (D): monto recibido menor al monto a cobrar — mensaje EXACTO del documento, sin punto final', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('100');
            cy.contains('button', 'Registrar ingreso de caja').click();

            // Texto exacto del documento (paso 6): SIN punto final. El código
            // real agrega un "." al final del mensaje.
            cy.get('#msg').should('have.text', 'El monto recibido (Q100) es menor al monto a cobrar (Q150)');
            // No debe permitir continuar (postcondición del paso): sigue en el
            // formulario de cobro, no se muestra el comprobante.
            cy.get('#cobro-area').should('be.visible');
            cy.get('#area-comprobante').should('have.class', 'd-none');
        });

        it('NOTA GAP (E): al calcular el cambio debe mostrar "Monto recibido: Q[monto]. Cambio a devolver: Q[cambio]."', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');

            // Texto exacto del documento (paso 7). La app real solo muestra
            // "Cambio a devolver: Q50.00", sin la primera oración.
            cy.get('#cambio-vivo').should('have.text', 'Monto recibido: Q200. Cambio a devolver: Q50.00.');
        });

        it('NOTA GAP (C): el botón de confirmar el cobro debe decir "Registrar Pago"', () => {
            irAPantallaDeCobro();
            // Texto exacto pedido por el documento (pasos 6 y FA01-4). El botón
            // real dice "Registrar ingreso de caja".
            cy.contains('button', 'Registrar Pago').should('exist');
        });

        it('Paso 8 (contrato del frontend): muestra el mensaje EXACTO del documento cuando el backend lo devuelve así', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');

            const mensajeExacto = '¡Pago registrado exitosamente! Paciente: Ana López. '
                + 'La cita ha sido actualizada a estado Confirmada.';

            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: {
                    numeroTransaccion: 'TRX-000456',
                    pacienteNombre: citaPendiente.pacienteNombre,
                    monto: citaPendiente.monto,
                    metodoPago: 'EFECTIVO',
                    cambio: '50.00',
                    mensaje: mensajeExacto,
                },
            }).as('cobro');

            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro');

            cy.get('#msg').should('contain.text', mensajeExacto);
        });

        it('Paso 9: el comprobante muestra los datos de la transacción', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');

            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: {
                    numeroTransaccion: 'TRX-000456',
                    pacienteNombre: citaPendiente.pacienteNombre,
                    monto: citaPendiente.monto,
                    metodoPago: 'EFECTIVO',
                    cambio: '50.00',
                    mensaje: 'ok',
                },
            }).as('cobro');

            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro');

            cy.get('#area-comprobante').should('be.visible');
            cy.get('#cobro-area').should('have.class', 'd-none');
            cy.get('#rec-trx').should('have.text', 'TRX-000456');
            cy.get('#rec-pac').should('have.text', citaPendiente.pacienteNombre);
            cy.get('#rec-tot').should('have.text', String(citaPendiente.monto));
            cy.get('#rec-met').should('have.text', 'EFECTIVO');
            cy.get('#rec-cam').should('have.text', '50.00');
        });

        it('Paso 10: el Empleado Interno puede imprimir el comprobante', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');
            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: { numeroTransaccion: 'TRX-000456', pacienteNombre: citaPendiente.pacienteNombre, monto: citaPendiente.monto, metodoPago: 'EFECTIVO', cambio: '50.00', mensaje: 'ok' },
            }).as('cobro');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro');

            cy.window().then((win) => cy.stub(win, 'print').as('print'));
            cy.contains('button', 'Imprimir recibo').click();
            cy.get('@print').should('have.been.called');
        });

        it('NOTA GAP (F): el botón para iniciar un nuevo cobro debe decir "Nuevo Cobro"', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');
            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: { numeroTransaccion: 'TRX-000456', pacienteNombre: citaPendiente.pacienteNombre, monto: citaPendiente.monto, metodoPago: 'EFECTIVO', cambio: '50.00', mensaje: 'ok' },
            }).as('cobro');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro');

            // Texto exacto del documento (paso 10). El botón real dice
            // "Nuevo cobro" (minúscula).
            cy.contains('button', 'Nuevo Cobro').should('exist');
        });

        it('Paso 10 (comportamiento real, minúscula incluida): "Nuevo cobro" reinicia la pantalla de búsqueda', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');
            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: { numeroTransaccion: 'TRX-000456', pacienteNombre: citaPendiente.pacienteNombre, monto: citaPendiente.monto, metodoPago: 'EFECTIVO', cambio: '50.00', mensaje: 'ok' },
            }).as('cobro');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro');

            cy.contains('button', 'Nuevo cobro').click();
            cy.get('#area-comprobante').should('have.class', 'd-none');
            cy.get('#area-busqueda').should('not.have.class', 'd-none');
            cy.get('#refId').should('have.value', '');
            cy.get('#refDpi').should('have.value', '');
        });
    });

    describe('FA01 - Pago con tarjeta de crédito o débito', () => {
        beforeEach(() => {
            abrirPantallaDeCaja();
            irAPantallaDeCobro();
        });

        it('Pasos 1-5: selecciona tipo de tarjeta, ingresa los últimos 4 dígitos y continúa en el paso 8 del flujo normal', () => {
            cy.get('#metodo').select('VISA');
            cy.get('#div-tarjeta').should('be.visible');
            cy.get('#ultimos').type('1234');

            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: { numeroTransaccion: 'TRX-000789', pacienteNombre: citaPendiente.pacienteNombre, monto: citaPendiente.monto, metodoPago: 'VISA', cambio: '0.00', mensaje: 'ok' },
            }).as('cobro');

            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@cobro').its('request.body').should('deep.include', { ultimosCuatroDigitos: '1234' });
            cy.get('#area-comprobante').should('be.visible');
        });

        it('exige los últimos 4 dígitos de la tarjeta antes de permitir el cobro', () => {
            cy.get('#metodo').select('MASTERCARD');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.get('#msg').should('contain.text', 'Ingrese los últimos 4 dígitos de la tarjeta.');
            cy.get('#cobro-area').should('be.visible');
        });
    });

    describe('FA02 - No se encuentran citas pendientes', () => {
        beforeEach(abrirPantallaDeCaja);

        it('muestra el mensaje EXACTO del documento cuando no hay resultados', () => {
            cy.intercept('GET', '/api/caja/citas/buscar*', []).as('buscarVacio');
            cy.get('#tipoServ').select('Consulta médica');
            cy.get('#refId').type('999');
            cy.contains('button', 'Buscar cuentas').click();
            cy.wait('@buscarVacio');

            cy.get('#msg').should('have.text', 'No se encontraron citas pendientes de pago para el criterio ingresado.');
            cy.get('#tb-cuentas-container').should('have.class', 'd-none');
        });

        it('Paso 3-4: permite corregir el criterio e intentar de nuevo', () => {
            cy.intercept('GET', '/api/caja/citas/buscar*', []).as('buscarVacio');
            cy.get('#tipoServ').select('Consulta médica');
            cy.get('#refId').type('999');
            cy.contains('button', 'Buscar cuentas').click();
            cy.wait('@buscarVacio');

            cy.intercept('GET', '/api/caja/citas/buscar*', [citaPendiente]).as('buscarOk');
            cy.get('#refId').clear().type(String(citaPendiente.id));
            cy.contains('button', 'Buscar cuentas').click();
            cy.wait('@buscarOk');
            cy.contains('#tb-cuentas tr', citaPendiente.pacienteNombre).should('exist');
        });
    });

    describe('FA03 - El paciente no puede realizar el pago', () => {
        // NOTA: la cancelación automática a los 10 minutos (paso 4 de FA03) es
        // un job de backend que no se refleja en ningún elemento de
        // caja.html — no se puede probar desde este spec de UI.
        beforeEach(() => {
            abrirPantallaDeCaja();
            irAPantallaDeCobro();
        });

        it('Paso 2: si el Empleado Interno cancela, no se envía ningún cobro y la cita queda sin procesar', () => {
            cy.intercept('POST', '/api/caja/cobro').as('cobroSpy');
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');

            cy.contains('button', 'Cancelar').click();

            cy.get('#cobro-area').should('have.class', 'd-none');
            cy.get('#area-busqueda').should('not.have.class', 'd-none');
            cy.get('@cobroSpy.all').should('have.length', 0);
        });
    });

    describe('FA04 - Pago con tarjeta rechazado (contrato del frontend, no del backend real)', () => {
        // OJO: se simula que el backend YA responde con el texto exacto del
        // documento. Esto confirma que el frontend sabe mostrarlo y dejar
        // reintentar sin perder los datos de la cuenta — no confirma que el
        // backend real hoy devuelva ese texto exacto para el rechazo bancario.
        beforeEach(() => {
            abrirPantallaDeCaja();
            irAPantallaDeCobro();
        });

        it('muestra el mensaje EXACTO del documento y permite reintentar (continúa en el paso 4 del flujo normal)', () => {
            const mensajeRechazo = 'La transacción con tarjeta fue rechazada por el banco. '
                + 'Solicite al paciente otro método de pago.';

            cy.get('#metodo').select('VISA');
            cy.get('#ultimos').type('1234');
            cy.intercept('POST', '/api/caja/cobro', { statusCode: 402, body: { message: mensajeRechazo } }).as('rechazo');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@rechazo');

            cy.get('#msg').should('contain.text', mensajeRechazo);
            cy.get('#cobro-area').should('be.visible');
            cy.get('#area-comprobante').should('have.class', 'd-none');
            cy.contains('button', 'Registrar ingreso de caja').should('not.be.disabled');

            // El paciente cambia a efectivo (paso 4 del flujo normal) y se
            // completa el cobro sin perder los datos de la cuenta.
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('200');
            cy.intercept('POST', '/api/caja/cobro', {
                statusCode: 200,
                body: { numeroTransaccion: 'TRX-000999', pacienteNombre: citaPendiente.pacienteNombre, monto: citaPendiente.monto, metodoPago: 'EFECTIVO', cambio: '50.00', mensaje: 'ok' },
            }).as('reintento');
            cy.contains('button', 'Registrar ingreso de caja').click();
            cy.wait('@reintento');
            cy.get('#area-comprobante').should('be.visible');
        });
    });
});