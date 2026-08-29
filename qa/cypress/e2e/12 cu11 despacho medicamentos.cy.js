// Ubicación en tu proyecto: qa/cypress/e2e/12_CU11_despacho_medicamentos.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO 11_CU_Despacho_de_Medicamentos.docx
// (que a su vez cita RN-CU10-01 y RN-CU10-03) y contra el código real de
// DespachoFarmaciaController / DespachoFarmaciaServiceImpl / RecetaVigenteResponseDTO
// / farmacia_despacho.html. Donde el código se desvía del texto/las reglas del
// documento, la prueba queda tal cual el documento lo exige y por lo tanto VA A
// FALLAR -- con un comentario "NOTA GAP" explicando la diferencia encontrada. No
// se ajustó ningún selector, texto esperado ni mock para maquillar el resultado.
//
// AVISO DE NUMERACIÓN (léelo antes de nombrar el archivo definitivo):
// el .docx que me diste se llama "11_CU_Despacho_de_Medicamentos.docx" y TODO
// el código (controller/service/html) ya está comentado como "FIX CU-11" en
// más de diez lugares. PERO el documento individual usa las reglas RN-CU10-01
// y RN-CU10-03, y el spec 11 (cobro de laboratorio en caja) ya había dejado
// anotado que, según Reglas_de_Negocio_Consolidadas, "CU-10" es justamente
// "Despacho de Medicamentos" y "CU-09" es "Gestión de Laboratorio". O sea: el
// documento se autodenomina CU-10 por sus propias reglas de negocio, pero el
// código se desarrolló como CU-11. Dejé el describe() y el nombre de archivo
// en "CU-11" porque es lo que ya está implementado en todos lados, pero
// confirmá cuál es el número real antes de la entrega final -- esto también
// resuelve (parcialmente) la duda que había quedado pendiente en el spec 11.
//
// ============================= RESUMEN DE GAPS =============================
//   A) [Paso 13] La columna "Consulta" de la tabla de resultados pinta
//      r.medicoNombre (el nombre del médico), no un ID/referencia de consulta.
//      RecetaVigenteResponseDTO ni siquiera trae un campo de consulta/cita, así
//      que no hay forma de mostrar el dato real aunque se corrija el HTML.
//   B) [Paso 13] Falta la columna "Fecha de Emisión" que pide el documento
//      ADEMÁS de "Vigencia". Hoy solo se ve el cálculo de vigencia (días
//      transcurridos); la fecha real nunca se imprime, aunque el backend sí la
//      manda en el DTO (fechaEmision).
//   C) [Paso 14] El botón dice "Seleccionar"; el documento pide "Despachar".
//      Desviación de texto, no de comportamiento -- se deja como nota, sin test.
//   D) [Paso 14] Las recetas vencidas NO llegan a la tabla de resultados: el
//      backend las filtra por completo en buscarRecetasVigentes()
//      (.filter(this::esVigente) antes de mapear). El documento pide que SÍ
//      aparezcan, pero con el botón deshabilitado y la etiqueta roja "Vencida".
//      Se prueba que el FRONTEND sabe renderizar ese estado si recibiera una
//      receta vencida (la lógica ya existe y funciona), pero en producción esa
//      rama nunca se ejecuta porque el backend nunca la manda.
//   E) [Paso 15] El detalle de medicamentos nunca muestra "precio unitario" en
//      pantalla (sí se usa internamente para calcular el total, pero no se
//      pinta en ninguna celda), aunque el documento lo pide explícitamente.
//   F) [Paso 15] El mensaje de receta vencida que arma
//      DespachoFarmaciaServiceImpl.despachar() ("La receta es inválida o está
//      vencida (máximo 7 días).") no coincide con el texto exacto del
//      documento ("Receta Vencida. La receta #[ID] fue emitida hace [X] días y
//      ya no es válida para despacho."). No genera test E2E aparte: dado el
//      gap D, esta rama del backend hoy es prácticamente inalcanzable desde la
//      UI (nunca llega una receta vencida hasta el botón de despachar).
//   G) [FA01] La alerta de stock bajo NO incluye el nombre del medicamento
//      ("[Nombre]: Stock bajo..."), solo dice "Stock bajo — disponible...".
//   H) [FA02] El mensaje de sustitución dice "...será notificado." en vez de
//      "...será notificado de la sustitución." (texto exacto del documento).
//   I) [FA04] El mensaje de alerta de stock mínimo que arma el backend
//      ("ALERTA: El medicamento X alcanzó stock mínimo.") no incluye ni las
//      unidades restantes ni la recomendación de reabastecer que pide el
//      documento textualmente.
//   J) [Paso 22] El resumen del despacho (detalle de medicamentos despachados +
//      monto total) que pide el documento nunca se pinta en pantalla: el
//      backend YA lo manda completo en
//      DespachoFarmaciaResponseDTO.medicamentosDespachados, pero
//      farmacia_despacho.html lo ignora por completo y solo muestra el texto
//      plano de "mensaje".
//
// ========================= CONFIRMADO CORRECTO (sin gap) =========================
//   - Paso 16 (disponibilidad de inventario por sucursal) y FA01 mensaje "Sin
//     inventario registrado" -- coinciden exacto.
//   - Paso 18 "Total del Despacho: Q[monto]." -- coincide exacto.
//   - Paso 19 / nota de que el cobro físico queda fuera del sistema -- coincide.
//   - Paso 20 botón "Confirmar Despacho" -- coincide.
//   - Paso 23 mensaje de éxito "Despacho registrado exitosamente. [X]
//     medicamento(s) despachado(s). Total: Q[monto]." -- confirmado como string
//     literal exacto en DespachoFarmaciaServiceImpl.
//   - FA02 campo "Razón de sustitución" obligatorio al elegir sustituto --
//     coincide.
//   - FA03 mensaje completo de abandono -- coincide EXACTO, y es la misma
//     función de mensaje que ya se había confirmado en el spec de Cobro de
//     Laboratorio en Caja.

describe('CU-11 - Despacho de Medicamentos', () => {

    // Fechas relativas al momento de correr el spec (evita que el test se
    // vuelva flaky con el paso de los días, a diferencia de usar fechas fijas).
    const isoHaceDias = (dias) => new Date(Date.now() - dias * 24 * 60 * 60 * 1000).toISOString();

    const recetaVigente = {
        id: 501,
        pacienteNombre: 'Julio César Bran',
        medicoNombre: 'Dra. Silvia Morán',
        fechaEmision: isoHaceDias(2),
        cantidadMedicamentos: 2,
    };

    const detalleRecetaVigente = {
        id: 501,
        citaId: 300,
        pacienteNombre: 'Julio César Bran',
        medicoNombre: 'Dra. Silvia Morán',
        fechaEmision: isoHaceDias(2),
        notas: '',
        activo: true,
        medicamentos: [
            {
                id: 900, medicamentoId: 10, medicamentoNombre: 'Paracetamol 500mg',
                dosis: '1 tableta cada 8 horas', frecuencia: 'Cada 8 horas', duracion: '5 días',
                indicaciones: 'Tomar con alimentos', cantidad: 15, precioUnitario: 2.50, subtotal: 37.50,
            },
            {
                id: 901, medicamentoId: 11, medicamentoNombre: 'Amoxicilina 500mg',
                dosis: '1 cápsula cada 12 horas', frecuencia: 'Cada 12 horas', duracion: '7 días',
                indicaciones: '', cantidad: 14, precioUnitario: 3.00, subtotal: 42.00,
            },
        ],
    };

    const inventarioSede = [
        { id: 1, medicamentoId: 10, medicamentoNombre: 'Paracetamol 500mg', sucursalNombre: 'Sede Central', stockActual: 20, stockMinimo: 10, alertaStockBajo: false, medicamentoControlado: false },
        { id: 2, medicamentoId: 11, medicamentoNombre: 'Amoxicilina 500mg', sucursalNombre: 'Sede Central', stockActual: 5, stockMinimo: 10, alertaStockBajo: true, medicamentoControlado: false },
    ];

    const medicamentosCatalogo = [
        { id: 10, nombre: 'Paracetamol 500mg', precio: 2.50, activo: true },
        { id: 11, nombre: 'Amoxicilina 500mg', precio: 3.00, activo: true },
        { id: 12, nombre: 'Ibuprofeno 400mg', precio: 2.00, activo: true },
    ];

    const abrirBusqueda = () => {
        cy.simularSesion({ rol: 'Farmaceutico', nombre: 'Lic. Marta Solís', uid: 70 });
        cy.visit('/farmacia_despacho.html');
    };

    const buscarPorRecetaId = (id, resultados) => {
        cy.intercept('GET', `/api/farmacia/recetas/buscar?recetaId=${id}`, resultados).as('buscar');
        cy.get('#criterioTipo').select('ID de Receta');
        cy.get('#criterio').type(String(id));
        cy.contains('button', 'Buscar receta').click();
        cy.wait('@buscar');
    };

    const abrirDetalle = (id, receta = detalleRecetaVigente, inventario = inventarioSede) => {
        cy.intercept('GET', '/api/medicamentos', medicamentosCatalogo).as('catalogo');
        cy.intercept('GET', '/api/usuarios/me', { sucursalId: 5 }).as('me');
        cy.intercept('GET', '/api/inventario?sucursalId=5', inventario).as('inventario');
        cy.intercept('GET', `/api/farmacia/recetas/${id}/detalle`, receta).as('detalle');
        cy.contains('#tb-recetas tr', String(id)).contains('button', 'Seleccionar').click();
        cy.wait(['@catalogo', '@me', '@inventario', '@detalle']);
    };

    describe('Flujo normal básico (pasos 12-24 del documento)', () => {

        it('Paso 13: la pantalla de búsqueda permite filtrar por ID de Receta y por ID de Consulta', () => {
            abrirBusqueda();
            cy.get('#criterioTipo').should('contain.text', 'ID de Receta').and('contain.text', 'ID de Consulta');
            // Nota: el frontend además ofrece "DPI del paciente" como tercer
            // criterio. Es una extensión intencional respecto al documento
            // (que solo pide ID de Receta e ID de Consulta), no un bug.
        });

        it('Paso 13: al buscar, se listan las recetas activas encontradas', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            cy.get('#area-recetas').should('not.have.class', 'd-none');
            cy.contains('#tb-recetas tr', recetaVigente.pacienteNombre).should('exist');
        });

        it('NOTA GAP (A): la columna "Consulta" debería mostrar el ID/referencia de la consulta, no el nombre del médico', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            cy.get('#tb-recetas tr').first().find('td').eq(2)
                .invoke('text')
                .should('match', /^\s*\d+\s*$/); // GAP: hoy contiene "Dra. Silvia Morán" (medicoNombre)
        });

        it('NOTA GAP (B): la tabla de resultados debería mostrar también la "Fecha de Emisión" de cada receta, además de la Vigencia', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            const fechaVisible = recetaVigente.fechaEmision.slice(0, 10); // yyyy-mm-dd
            cy.get('#tb-recetas').invoke('text').should('include', fechaVisible); // GAP: nunca se imprime la fecha, solo "Vigente (2 días)"
        });

        it('Paso 15: al seleccionar una receta vigente se muestra el detalle con nombre, dosis y cantidad recetada de cada medicamento', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.get('#area-despacho').should('not.have.class', 'd-none');
            // Cantidad inicial en el input = min(cantidad recetada, stock en sede):
            // Paracetamol min(15, 20) = 15 -- Amoxicilina min(14, 5) = 5
            cy.contains('#tb-detalle tr', 'Paracetamol 500mg').within(() => {
                cy.contains('1 tableta cada 8 horas').should('exist');
                cy.get('.c-cant').should('have.value', '15');
            });
            cy.contains('#tb-detalle tr', 'Amoxicilina 500mg').within(() => {
                cy.contains('1 cápsula cada 12 horas').should('exist');
                cy.get('.c-cant').should('have.value', '5');
            });
        });

        it('NOTA GAP (E): el detalle de medicamentos debería mostrar también el precio unitario de cada uno', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.get('#area-despacho').invoke('text').should('include', 'Q2.50'); // GAP: el precio unitario nunca se pinta en pantalla
        });

        it('Paso 16: el sistema consulta el inventario de la sucursal y muestra la disponibilidad de cada medicamento', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.contains('#tb-detalle tr', 'Paracetamol 500mg').should('contain.text', '20 ud.');
            cy.contains('#tb-detalle tr', 'Amoxicilina 500mg').should('contain.text', '5 ud.');
        });

        it('Paso 18: el sistema calcula y muestra "Total del Despacho: Q[monto]" en tiempo real al ajustar cantidades', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            // 15 * 2.50 + 5 * 3.00 = 37.50 + 15.00 = 52.50 (Amoxicilina se limita al stock: 5 ud.)
            cy.get('#lbl-total').should('have.text', '52.50');
            cy.contains('h3', 'Total del Despacho: Q').should('exist');
        });

        it('Paso 19: la pantalla indica que el cobro físico es responsabilidad de farmacia, fuera del sistema', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.contains('El cobro físico es responsabilidad de farmacia fuera de este sistema.').should('be.visible');
        });

        it('Pasos 20-23: al confirmar el despacho se envía recetaId + items y se muestra el mensaje de éxito exacto del documento', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);

            cy.intercept('POST', '/api/farmacia/despacho', {
                numeroTransaccion: 'abc-123',
                recetaId: 501,
                pacienteNombre: 'Julio César Bran',
                medicamentosDespachados: [],
                alertasStock: [],
                mensaje: 'Despacho registrado exitosamente. 2 medicamento(s) despachado(s). Total: Q52.50.',
            }).as('despachar');

            cy.contains('button', 'Confirmar Despacho').click();
            cy.wait('@despachar').its('request.body').should('deep.include', { recetaId: 501 });
            cy.get('@despachar').its('request.body.items').should('have.length', 2);
            cy.get('#msg').should('contain.text', 'Despacho registrado exitosamente. 2 medicamento(s) despachado(s). Total: Q52.50.');
        });

        it('NOTA GAP (J): tras confirmar el despacho, el sistema debería mostrar el resumen con el detalle de medicamentos despachados', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);

            cy.intercept('POST', '/api/farmacia/despacho', {
                numeroTransaccion: 'abc-123',
                recetaId: 501,
                pacienteNombre: 'Julio César Bran',
                medicamentosDespachados: [
                    { id: 900, medicamentoNombre: 'Paracetamol 500mg', cantidad: 15, precioUnitario: 2.50, subtotal: 37.50 },
                    { id: 901, medicamentoNombre: 'Amoxicilina 500mg', cantidad: 5, precioUnitario: 3.00, subtotal: 15.00 },
                ],
                alertasStock: [],
                mensaje: 'Despacho registrado exitosamente. 2 medicamento(s) despachado(s). Total: Q52.50.',
            }).as('despachar');

            cy.contains('button', 'Confirmar Despacho').click();
            cy.wait('@despachar');
            // GAP: el backend ya manda medicamentosDespachados, pero la pantalla
            // solo muestra el texto de "mensaje" -- nunca pinta un resumen/tabla.
            // Se verifica puntualmente dentro de #msg (el único elemento que se
            // actualiza tras el despacho) para no dar falso positivo con la fila
            // que queda en el DOM, oculta, dentro de la tabla #tb-detalle.
            cy.get('#msg').invoke('text').should('include', 'Paracetamol 500mg');
        });
    });

    describe('FA01 - Medicamento sin inventario disponible / stock bajo', () => {

        it('Mensaje exacto "Sin inventario registrado" cuando el medicamento no tiene fila de inventario en la sucursal', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id, detalleRecetaVigente, [inventarioSede[0]]); // Amoxicilina sin inventario
            cy.contains('#tb-detalle tr', 'Amoxicilina 500mg').should('contain.text', 'Sin inventario registrado');
        });

        it('NOTA GAP (G): la alerta de stock bajo debería iniciar con el nombre del medicamento ("[Nombre]: Stock bajo...")', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id); // Amoxicilina: stockActual 5 <= stockMinimo 10
            cy.contains('#tb-detalle tr', 'Amoxicilina 500mg')
                .should('contain.text', 'Amoxicilina 500mg: Stock bajo — disponible: 5 (mínimo: 10)');
            // GAP: el texto real es "Stock bajo — disponible: 5 (mínimo: 10)", sin el nombre al inicio.
        });
    });

    describe('FA02 - Sustitución de medicamento', () => {

        it('Al elegir un sustituto se habilita y vuelve obligatorio el campo "Razón de sustitución"', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.contains('#tb-detalle tr', 'Paracetamol 500mg').within(() => {
                cy.get('.c-sus').select('Ibuprofeno 400mg');
                cy.get('.c-razon').should('not.have.class', 'd-none').and('have.attr', 'required');
            });
        });

        it('NOTA GAP (H): el aviso de sustitución debería decir "...será notificado DE LA SUSTITUCIÓN", no solo "...será notificado."', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);
            cy.contains('#tb-detalle tr', 'Paracetamol 500mg').find('.c-sus').select('Ibuprofeno 400mg');
            cy.get('#msg').should('contain.text', 'Medicamento Paracetamol 500mg sustituido por Ibuprofeno 400mg. El médico tratante será notificado de la sustitución.');
            // GAP: el texto real termina en "...será notificado." (sin "de la sustitución").
        });
    });

    describe('FA03 - El paciente no desea adquirir los medicamentos', () => {

        it('Pide confirmación y, al aceptar, muestra el mensaje exacto de abandono y cierra el detalle', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);

            cy.contains('button', 'Registrar abandono').click();
            cy.get('#app-confirm-mensaje').should('contain.text', 'abandonó/rechazó la compra');

            cy.intercept('POST', '/api/farmacia/recetas/501/rechazar', {
                mensaje: 'Se ha registrado que el paciente Julio César Bran no adquirió los medicamentos recetados en farmacia interna. Receta: 501',
            }).as('rechazar');

            cy.get('#app-confirm-aceptar').click();
            cy.wait('@rechazar');
            cy.get('#msg').should('contain.text', 'Se ha registrado que el paciente Julio César Bran no adquirió los medicamentos recetados en farmacia interna. Receta: 501');
            cy.get('#area-despacho').should('have.class', 'd-none');
        });
    });

    describe('FA04 - Stock mínimo alcanzado tras el despacho', () => {

        it('NOTA GAP (I): la alerta post-despacho debería indicar unidades restantes y recomendar reabastecimiento', () => {
            abrirBusqueda();
            buscarPorRecetaId(recetaVigente.id, [recetaVigente]);
            abrirDetalle(recetaVigente.id);

            cy.intercept('POST', '/api/farmacia/despacho', {
                numeroTransaccion: 'abc-124',
                recetaId: 501,
                medicamentosDespachados: [],
                alertasStock: ['ALERTA: El medicamento Amoxicilina 500mg alcanzó stock mínimo.'], // texto REAL tal como lo arma el ServiceImpl
                mensaje: 'Despacho registrado exitosamente. 2 medicamento(s) despachado(s). Total: Q52.50.',
            }).as('despacharConAlerta');

            cy.contains('button', 'Confirmar Despacho').click();
            cy.wait('@despacharConAlerta');
            cy.get('#msg').should('contain.text', 'ALERTA: El medicamento Amoxicilina 500mg ha alcanzado el nivel de stock mínimo (0 unidades restantes). Se recomienda generar orden de reabastecimiento.');
            // GAP: el backend hoy solo manda "ALERTA: El medicamento Amoxicilina 500mg alcanzó stock mínimo."
        });
    });

    describe('Paso 14 / FA implícito - Recetas vencidas', () => {

        it('NOTA GAP (D): si el backend mandara una receta vencida, el frontend la muestra con badge rojo "Vencida" y el botón deshabilitado (pero en producción esto nunca ocurre, ver gap D)', () => {
            const recetaVencida = { ...recetaVigente, id: 777, fechaEmision: isoHaceDias(18) };
            abrirBusqueda();
            buscarPorRecetaId(recetaVencida.id, [recetaVencida]);
            cy.contains('#tb-recetas tr', '777').within(() => {
                cy.get('.badge-danger').should('contain.text', 'Vencida');
                cy.contains('button', 'Seleccionar').should('be.disabled');
            });
        });
    });
});