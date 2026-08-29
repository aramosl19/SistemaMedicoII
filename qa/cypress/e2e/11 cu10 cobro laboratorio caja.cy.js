// Ubicación sugerida en tu proyecto: qa/cypress/e2e/10_CU_Cobro_Laboratorio_Caja.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 10_CU_Cobro_de_Laboratorio_en_Caja.docx
// y contra Reglas_de_Negocio_Consolidadas.docx (RN-GLOBAL-001, RN-GLOBAL-004,
// RN-GLOBAL-005). Donde caja.html / CobroLaboratorioServiceImpl se desvían del
// texto/las reglas del documento, la prueba queda tal cual el documento lo
// exige y por lo tanto VA A FALLAR — con un comentario "NOTA GAP" explicando
// la diferencia encontrada. No se ajustó ningún selector, texto esperado ni
// mock para maquillar el resultado.
//
// REVISIÓN 2: se corrigieron bugs del PROPIO spec (no del código real), y se
// re-etiquetaron los gaps que ya se cerraron en caja.html / DTOs / ServiceImpl
// desde la primera versión de este archivo:
//
//   BUG DE SPEC (no era gap real): tanto el botón de búsqueda (#btn-buscar)
//   como el de confirmar pago (#btn-confirmar-pago) cambian de texto según
//   tipoServ ("Buscar cuentas"/"Buscar" y "Registrar Pago"/"Confirmar Pago
//   Q[monto]"). Como este spec siempre trabaja en contexto de laboratorio
//   (tipoServ = LAB), el texto real SIEMPRE es "Buscar" y "Confirmar Pago
//   Q[monto]" — nunca "Buscar cuentas" ni "Registrar Pago". La versión
//   anterior de este spec usaba el texto fijo equivocado en los clics de
//   acción, lo que rompía casi toda la suite en cascada. Se corrigió
//   usando los ids (#btn-buscar, #btn-confirmar-pago) para hacer clic, y
//   se dejó la verificación de texto exacto solo en los tests dedicados a
//   eso (antes gaps I y E).
//
//   GAPS YA CERRADOS (confirmado contra caja.html + DTOs + ServiceImpl
//   reales) → sus tests dejaron de ser "NOTA GAP" y ahora son tests
//   normales que confirman el comportamiento correcto:
//     C) Columna DPI agregada a la tabla de resultados (además de
//        Cantidad de exámenes y Fecha, que ya estaban). Backend ya
//        poblaba pacienteDpi/cantidadExamenes en buscarOrdenesPendientes().
//     D) #c-cant-examenes agregado al resumen de cobro (el DPI ya estaba).
//     E) Botón de confirmar ya dice "Confirmar Pago Q[monto]" en laboratorio.
//     F) [RN-GLOBAL-005] Sucursal ya viaja en CobroLaboratorioResponseDTO
//        y se pinta en #rec-sucursal.
//     G) Mensaje de "sin resultados" ya coincide EXACTO con el documento.
//     H) Label ya dice "Monto Recibido (Q)" en contexto de laboratorio.
//     I) Botón de búsqueda ya dice "Buscar" en contexto de laboratorio.
//
//   GAP TODAVÍA ABIERTO:
//     B) [RN-GLOBAL-001] Sigue sin existir validación de DPI (13 dígitos,
//        solo numérico) ni en frontend ni en backend. Se mantiene como
//        NOTA GAP.
//
//   NUEVO REQUISITO (pedido explícitamente por el usuario, no viene del
//   documento original): el comprobante también debe indicar la cantidad
//   de exámenes que se pagaron. El HTML y el JS de caja.html ya están
//   listos (#rec-cant-examenes, poblado desde data.cantidadExamenes), PERO
//   el backend (CobroLaboratorioResponseDTO / CobroLaboratorioServiceImpl)
//   TODAVÍA NO tiene ese campo — quedó pendiente de que se agregue
//   manualmente. Por eso este test se deja como contrato de frontend sin
//   confirmar contra el backend real (igual que se hizo en su momento con
//   el mensaje de éxito de CU-06), y no como un "Paso" ya cerrado.
//
// AVISO DE NUMERACIÓN (léelo antes de nombrar el archivo definitivo):
// el .docx individual empieza con "10_", pero en Reglas_de_Negocio_Consolidadas
// el "CU-10" ya está asignado a "Despacho de Medicamentos" y el "CU-09" a
// "Gestión de Laboratorio" (resultados, cubierto por el spec 10 existente).
// Ninguno de los dos coincide con este caso de uso (cobro en caja). Dejé el
// archivo sin número de CU en el nombre hasta que confirmes cuál es el
// correcto — así evitamos pisar CU-09 o CU-10 reales.
//
//  A) Paso 1: el documento pide una pantalla dedicada "Cobro de Laboratorio
//     en Caja" accesible con un botón "Cobro Lab" desde el Panel de Caja.
//     La app real generaliza todo en un único "Módulo de Caja Centralizada"
//     (caja.html) con un selector "Tipo de servicio" (Consulta / Laboratorio).
//     Es la misma extensión intencional que ya se documentó en el spec de
//     CU-06 — no es un bug, pero es una desviación literal del documento.
//     No genera test (no hay nada verificable sin inventar una pantalla).
//
//  J) Paso 2: el documento pide seleccionar explícitamente un criterio de
//     búsqueda ("Por DPI" o "Por No. Orden") y luego llenar un solo campo.
//     La UI real no tiene ese selector: muestra los dos campos (ID de
//     referencia y DPI) a la vez y usa el que se haya llenado. Funciona,
//     pero no calza literalmente con el flujo descrito. Sigue sin
//     resolverse; no genera test nuevo (ya se documentó, se deja tal cual
//     se acordó anteriormente).
//
// NOTA sobre el mensaje de éxito (paso 9): se confirmó en
// CobroLaboratorioServiceImpl que el backend arma literalmente el string
// "¡Pago de laboratorio registrado exitosamente! Paciente: [Nombre]. La
// orden ha sido actualizada a estado 'En proceso'." — coincide exacto con
// el documento.
//
// NOTA sobre FA02 (el paciente no puede pagar): es un flujo manual — el
// cajero simplemente no procesa el cobro y la orden queda "Pendiente". No
// hay ningún mensaje de sistema que probar, así que no genera tests.
//
// NOTA sobre FA04 (tarjeta rechazada): el rechazo está simulado con un
// valor mágico de "últimos 4 dígitos" = "0000" (ver PagoTarjetaStrategy).
// Es una simulación esperable en un proyecto de curso sin pasarela real,
// no una desviación del documento — el mensaje que produce SÍ coincide
// exacto con el FA04 del documento.

describe('CU - Cobro de Laboratorio en Caja', () => {
    const ordenPendiente = {
        id: 501,
        pacienteNombre: 'Julio César Bran',
        pacienteDpi: '1234567890123',
        cantidadExamenes: 3,
        medicoNombre: 'Dra. Silvia Morán',
        especialidadNombre: 'Medicina Interna',
        fechaHora: '2026-08-27T09:00:00',
        estado: 'Pendiente',
        montoTotal: 350.0,
        fechaCreacion: '2026-08-25T14:10:00',
    };

    const abrirPantallaDeCaja = () => {
        cy.simularSesion({ rol: 'Cajero', nombre: 'Carlos Ramírez' });
        cy.visit('/caja.html');
        cy.get('#tipoServ').select('Exámenes de laboratorio');
    };

    const buscarPorNumeroOrden = () => {
        cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', [ordenPendiente]).as('buscar');
        cy.get('#refId').type(String(ordenPendiente.id));
        cy.get('#btn-buscar').click();
        cy.wait('@buscar');
    };

    const irAPantallaDeCobro = () => {
        buscarPorNumeroOrden();
        cy.contains('#tb-cuentas tr', ordenPendiente.pacienteNombre).contains('button', 'Cobrar').click();
        cy.get('#cobro-area').should('be.visible');
    };

    const mockCobroExitoso = (overrides = {}) => cy.intercept('POST', '/api/caja/laboratorio/cobro', {
        statusCode: 200,
        body: {
            numeroTransaccion: 'TRX-LAB-0001',
            ordenId: ordenPendiente.id,
            pacienteNombre: ordenPendiente.pacienteNombre,
            sucursal: 'Laboratorio',
            monto: ordenPendiente.montoTotal,
            metodoPago: 'EFECTIVO',
            montoRecibido: 400,
            cambio: 50.0,
            mensaje: 'ok',
            ...overrides,
        },
    }).as('cobro');

    describe('Flujo normal básico (pasos 1-12 del documento)', () => {
        beforeEach(abrirPantallaDeCaja);

        it('Paso 2 [RN-GLOBAL-001]: busca la orden por número de orden', () => {
            buscarPorNumeroOrden();
            cy.get('#tb-cuentas-container').should('be.visible');
            cy.contains('#tb-cuentas tr', ordenPendiente.pacienteNombre).should('exist');
        });

        it('Paso 2 [RN-GLOBAL-001]: busca la orden por DPI del paciente', () => {
            cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', [ordenPendiente]).as('buscar');
            cy.get('#refDpi').type('1234567890123');
            cy.get('#btn-buscar').click();
            cy.wait('@buscar');
            cy.contains('#tb-cuentas tr', ordenPendiente.pacienteNombre).should('exist');
        });

        it('Paso 2 (gap B cerrado) [RN-GLOBAL-001]: un DPI que no tiene 13 dígitos se rechaza antes de buscar, sin llamar a la API', () => {
            cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', cy.spy().as('llamadaApi'));
            cy.get('#refDpi').type('123ABC'); // el input filtra letras, queda "123"
            cy.get('#btn-buscar').click();
            cy.get('#msg').should('contain.text', 'El DPI debe contener exactamente 13 dígitos');
            cy.get('@llamadaApi').should('not.have.been.called');
        });

        it('Paso 2 (gap I cerrado): el botón de búsqueda dice "Buscar" en contexto de laboratorio', () => {
            cy.get('#btn-buscar').should('have.text', 'Buscar');
        });

        it('Paso 3 (gap C cerrado): la tabla de resultados muestra DPI, cantidad de exámenes y fecha de creación', () => {
            buscarPorNumeroOrden();
            cy.contains('#tb-cuentas-container th', 'DPI').should('exist');
            cy.contains('#tb-cuentas-container th', 'Exámenes').should('exist');
            cy.contains('#tb-cuentas-container th', 'Fecha').should('exist');
            cy.contains('#tb-cuentas tr', ordenPendiente.pacienteNombre).within(() => {
                cy.contains('td', ordenPendiente.pacienteDpi).should('exist');
                cy.contains('td', String(ordenPendiente.cantidadExamenes)).should('exist');
            });
        });

        it('Paso 4: selecciona la orden correspondiente y avanza al formulario de cobro', () => {
            irAPantallaDeCobro();
        });

        it('Paso 5: el resumen de cobro sí muestra el monto total y el número de orden', () => {
            irAPantallaDeCobro();
            cy.get('#c-monto').should('contain.text', String(ordenPendiente.montoTotal));
            cy.get('#c-id-text').should('have.text', String(ordenPendiente.id));
        });

        it('Paso 5 (gap D cerrado): el resumen de cobro muestra también el DPI y la cantidad de exámenes', () => {
            irAPantallaDeCobro();
            cy.get('#c-dpi').should('have.text', ordenPendiente.pacienteDpi);
            cy.get('#c-cant-examenes').should('have.text', String(ordenPendiente.cantidadExamenes));
        });

        it('Paso 6 [RN-GLOBAL-004]: las opciones de método de pago son Efectivo, Visa, Mastercard o Débito', () => {
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

        it('Paso 7 (gap H cerrado): el campo de monto recibido se llama "Monto Recibido (Q)"', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#div-efectivo label').should('have.text', 'Monto Recibido (Q)');
        });

        it('Envío sin seleccionar método de pago muestra un mensaje claro (antes bloqueado en silencio por el navegador)', () => {
            irAPantallaDeCobro();
            cy.get('#btn-confirmar-pago').click();
            cy.get('#msg').should('contain.text', 'Seleccione un método de pago.');
            cy.get('#cobro-area').should('be.visible');
        });

        it('Envío en efectivo sin escribir el monto recibido muestra un mensaje claro (antes bloqueado en silencio por el navegador)', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#btn-confirmar-pago').click();
            cy.get('#msg').should('contain.text', 'Ingrese el monto recibido.');
            cy.get('#cobro-area').should('be.visible');
        });

        it('Paso 7: al calcular el cambio muestra "Monto recibido: Q[monto]. Cambio a devolver: Q[cambio]."', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');
            cy.get('#cambio-vivo').should('have.text', 'Monto recibido: Q400. Cambio a devolver: Q50.00.');
        });

        it('Paso 7: monto recibido menor al monto a cobrar muestra el mensaje EXACTO del documento', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('100');
            cy.get('#btn-confirmar-pago').click();

            cy.get('#msg').should('have.text', 'El monto recibido (Q100) es menor al monto a cobrar (Q350)');
            cy.get('#cobro-area').should('be.visible');
            cy.get('#area-comprobante').should('have.class', 'd-none');
        });

        it('Paso 8 (gap E cerrado): el botón de confirmar dice "Confirmar Pago Q[monto]" con el monto incluido', () => {
            irAPantallaDeCobro();
            cy.get('#btn-confirmar-pago').should('have.text', `Confirmar Pago Q${ordenPendiente.montoTotal}`);
        });

        it('Paso 9 (contrato confirmado contra CobroLaboratorioServiceImpl): muestra el mensaje EXACTO de éxito', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');

            const mensajeExacto = `¡Pago de laboratorio registrado exitosamente! Paciente: ${ordenPendiente.pacienteNombre}. `
                + `La orden ha sido actualizada a estado 'En proceso'.`;

            mockCobroExitoso({ mensaje: mensajeExacto });

            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');
            cy.get('#msg').should('contain.text', mensajeExacto);
        });

        it('Paso 10: el comprobante muestra transacción, paciente, monto, método y cambio', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');

            mockCobroExitoso();

            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');

            cy.get('#area-comprobante').should('be.visible');
            cy.get('#rec-trx').should('have.text', 'TRX-LAB-0001');
            cy.get('#rec-pac').should('have.text', ordenPendiente.pacienteNombre);
            cy.get('#rec-tot').should('have.text', String(ordenPendiente.montoTotal));
            cy.get('#rec-met').should('have.text', 'EFECTIVO');
            cy.get('#rec-cam').should('have.text', '50');
        });

        it('Paso 10 (gap F cerrado) [RN-GLOBAL-005]: el comprobante incluye el nombre de la sucursal', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');

            mockCobroExitoso({ sucursal: 'Laboratorio' });

            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');

            cy.get('#rec-sucursal').should('have.text', 'Laboratorio');
        });

        it('NOTA (nuevo requisito, backend pendiente): el comprobante debe indicar la cantidad de exámenes pagados', () => {
            // #rec-cant-examenes ya existe en caja.html y se llena desde
            // data.cantidadExamenes. Este test mockea esa respuesta a nivel
            // de frontend, pero el campo cantidadExamenes TODAVÍA NO existe
            // en CobroLaboratorioResponseDTO / CobroLaboratorioServiceImpl
            // reales — falta agregarlo en el backend para que esto sea
            // cierto en producción, no solo en el mock.
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');

            mockCobroExitoso({ cantidadExamenes: ordenPendiente.cantidadExamenes });

            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');

            cy.get('#rec-cant-examenes').should('have.text', String(ordenPendiente.cantidadExamenes));
        });

        it('Paso 11: puede imprimir el comprobante', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');
            mockCobroExitoso();
            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');

            cy.window().then((win) => cy.stub(win, 'print').as('print'));
            cy.contains('button', 'Imprimir recibo').click();
            cy.get('@print').should('have.been.called');
        });

        it('Paso 11: puede iniciar un nuevo cobro con el botón "Nuevo Cobro"', () => {
            irAPantallaDeCobro();
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');
            mockCobroExitoso();
            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');

            cy.contains('button', 'Nuevo Cobro').click();
            cy.get('#area-comprobante').should('have.class', 'd-none');
            cy.get('#area-busqueda').should('not.have.class', 'd-none');
        });
    });

    describe('FA01 - No se encuentran órdenes pendientes', () => {
        beforeEach(abrirPantallaDeCaja);

        it('Paso 3 (gap G cerrado): el mensaje de "sin resultados" es el texto EXACTO del documento', () => {
            cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', []).as('buscarVacio');
            cy.get('#refId').type('999');
            cy.get('#btn-buscar').click();
            cy.wait('@buscarVacio');

            cy.get('#msg').should(
                'have.text',
                'No se encontraron órdenes de laboratorio pendientes de pago. Verifique el DPI o número de orden e intente de nuevo.'
            );
        });

        it('Pasos 3-4: permite corregir el criterio e intentar de nuevo', () => {
            cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', []).as('buscarVacio');
            cy.get('#refId').type('999');
            cy.get('#btn-buscar').click();
            cy.wait('@buscarVacio');

            cy.intercept('GET', '/api/caja/laboratorio/ordenes/buscar*', [ordenPendiente]).as('buscarOk');
            cy.get('#refId').clear().type(String(ordenPendiente.id));
            cy.get('#btn-buscar').click();
            cy.wait('@buscarOk');
            cy.contains('#tb-cuentas tr', ordenPendiente.pacienteNombre).should('exist');
        });
    });

    describe('FA03 - Pago con tarjeta de crédito o débito', () => {
        beforeEach(() => {
            abrirPantallaDeCaja();
            irAPantallaDeCobro();
        });

        it('Pasos 1-3: selecciona tipo de tarjeta, ingresa los últimos 4 dígitos y continúa en el paso 8', () => {
            cy.get('#metodo').select('VISA');
            cy.get('#div-tarjeta').should('be.visible');
            cy.get('#ultimos').type('1234');

            mockCobroExitoso({ numeroTransaccion: 'TRX-LAB-0002', metodoPago: 'VISA', montoRecibido: null, cambio: null });

            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro').its('request.body').should('deep.include', { ultimosCuatroDigitos: '1234' });
            cy.get('#area-comprobante').should('be.visible');
        });

        it('Paso 3 [FA04]: exige exactamente 4 dígitos antes de permitir el cobro, con el mensaje EXACTO del documento', () => {
            cy.get('#metodo').select('MASTERCARD');
            cy.get('#btn-confirmar-pago').click();
            cy.get('#msg').should('contain.text', 'Ingrese los últimos 4 dígitos de la tarjeta.');
            cy.get('#cobro-area').should('be.visible');
        });
    });

    describe('FA04 - Pago con tarjeta rechazado', () => {
        beforeEach(() => {
            abrirPantallaDeCaja();
            irAPantallaDeCobro();
        });

        it('muestra el mensaje EXACTO del documento y permite reintentar (continúa en el paso 6 del flujo normal)', () => {
            const mensajeRechazo = 'La transacción con tarjeta fue rechazada por el banco. '
                + 'Solicite al paciente otro método de pago.';

            cy.get('#metodo').select('VISA');
            cy.get('#ultimos').type('0000'); // valor simulado de rechazo en PagoTarjetaStrategy
            cy.intercept('POST', '/api/caja/laboratorio/cobro', { statusCode: 402, body: { message: mensajeRechazo } }).as('rechazo');
            cy.get('#btn-confirmar-pago').click();
            cy.wait('@rechazo');

            cy.get('#msg').should('contain.text', mensajeRechazo);
            cy.get('#cobro-area').should('be.visible');
            cy.get('#area-comprobante').should('have.class', 'd-none');

            // El paciente cambia a efectivo (paso 6 del flujo normal) y se
            // completa el cobro sin perder los datos de la orden.
            cy.get('#metodo').select('EFECTIVO');
            cy.get('#montoRecibido').type('400');
            mockCobroExitoso({ numeroTransaccion: 'TRX-LAB-0003' });
            cy.get('#btn-confirmar-pago').click();
            cy.wait('@cobro');
            cy.get('#area-comprobante').should('be.visible');
        });
    });
});