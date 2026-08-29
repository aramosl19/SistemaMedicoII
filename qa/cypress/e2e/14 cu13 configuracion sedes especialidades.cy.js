// Ubicación sugerida: qa/cypress/e2e/14_CU13_configuracion_sedes_especialidades.cy.js
//
// Este spec está escrito CONTRA:
//  - 13_CU_Configuracion_Sedes_Especialidades.docx (flujo/textos de UI)
//  - Reglas_de_Negocio_Consolidadas.docx, RN-CU12-01 (el documento CU-13 cita
//    "RN-CU12-01" internamente; es numeración heredada de una versión
//    anterior, no un CU-12 distinto — mismo caso ya documentado en el spec de
//    CU-12/RN-CU11).
//
// DECISIONES CONFIRMADAS CON EDY (ya no son gaps, aunque el documento describa
// otra cosa — se dejan como comentario para que el próximo QA no las vuelva a
// levantar):
//  - GAP #1 (arquitectura): no existen /branch-specialty ni
//    /branch-specialty/create, TableServer, ni deleteBranchSpecialty(). Es una
//    opción del <select id="sel-catalogo"> en admin_catalogos.html, con el
//    modal genérico compartido por todos los catálogos. Mismo criterio ya
//    aceptado en CU-12: un solo módulo de catálogos en vez de pantallas/rutas
//    independientes por catálogo. Aceptado, no se migra a rutas separadas.
//
// GAPS REALES YA CORREGIDOS EN ESTA VUELTA:
//  - GAP #2: textos de botones y diálogo ajustados al documento
//    ("Asignar Especialidad", "Asignar", "Eliminar", "¿Confirmar
//    eliminación?"), de forma condicional según el catálogo activo, sin tocar
//    el texto de los demás catálogos.
//  - GAP #3: guardarRegistro() ahora lee `respuesta.mensaje` (el texto que ya
//    armaba SucursalEspecialidadController) en vez de construir un texto
//    genérico. Los demás catálogos, que no devuelven "mensaje", siguen
//    mostrando su texto genérico de siempre (fallback intacto).
//  - GAP #4 (RN-CU12-01, índice único): SucursalEspecialidad.java ahora tiene
//    un @UniqueConstraint real en (sucursal_id, especialidad_id).
//    SucursalEspecialidadServiceImpl.asignar() reutiliza la fila existente en
//    vez de insertar una nueva cada vez (para no romper "remover y volver a
//    asignar" contra un índice único no filtrado), y captura
//    DataIntegrityViolationException para convertir la condición de carrera
//    en el mismo DuplicateResourceException de siempre en vez de un 500.
//    Esto NO es verificable con un e2e mockeado (no hay BD real detrás de un
//    intercept) — se deja como test de integración pendiente, ver el describe
//    correspondiente más abajo.
//  - GAP #5: se quitó el `required` nativo de los <select> de Sede y
//    Especialidad; ahora toda la validación de campos obligatorios pasa por
//    el mismo camino que ya usa el resto del sistema (mensaje del backend vía
//    App.showAlert), sin depender del navegador.
//
// GAP #6 (NUEVO, este fix): "se cierra la sesión" a mitad de casi todos los
//    tests — cy.get('#sel-catalogo').select(...) fallaba con "element has
//    detached from the DOM" o el @listado nunca llegaba a ocurrir.
//    Causa real (ver el log de Routes en cada fallo): admin_catalogos.html
//    hace un GET /api/roles al cargar (junto con sedes/especialidades/
//    laboratorios), y ese endpoint NO estaba interceptado. Cypress lo deja
//    pasar al backend real, que responde 401 porque el token es falso
//    ('token-fake-sesion'), y el interceptor de fetch/App.js interpreta el
//    401 como sesión expirada → redirige solo a login.html?expired=true justo
//    mientras el test seguía interactuando con la página vieja. Fix: se
//    intercepta /api/roles igual que los demás catálogos y se espera junto
//    con el resto antes de tocar el DOM.

describe('CU-13 / RN-CU12-01 - Configuración de Sedes y Especialidades', () => {
    const sedes = [
        { id: 1, nombre: 'Sede Central', activo: true },
        { id: 2, nombre: 'Sede Zona 10', activo: true },
    ];
    const especialidades = [
        { id: 10, nombre: 'Medicina General', activo: true },
        { id: 11, nombre: 'Pediatría', activo: true },
    ];
    const roles = [
        { id: 1, nombre: 'Administrador', activo: true },
    ];

    const asignacionExistente = {
        id: 500,
        sucursalId: 1,
        sucursalNombre: 'Sede Central',
        especialidadId: 10,
        especialidadNombre: 'Medicina General',
        activo: true,
    };

    const abrirCatalogoConDatos = (listado = [asignacionExistente]) => {
        cy.intercept('GET', '/api/sucursales', sedes).as('sedes');
        cy.intercept('GET', '/api/especialidades', especialidades).as('especialidades');
        cy.intercept('GET', '/api/laboratorios', []).as('laboratorios');
        // GAP #6: faltaba este intercept. Sin él, /api/roles pega contra el
        // backend real, responde 401 con el token falso de la sesión
        // simulada, y la app redirige a login.html a mitad de los tests
        // (ver nota arriba). Se agrega y se espera junto con el resto.
        cy.intercept('GET', '/api/roles', roles).as('roles');
        cy.intercept('GET', '/api/sucursal-especialidad', listado).as('listado');
        // FIX (el token "no sobrevivía"): cy.simularSesion() usa cy.window()
        // ANTES de cy.visit(), y con test isolation (default en Cypress
        // moderno) cada it() arranca en about:blank, un origen aislado — el
        // localStorage que simularSesion() guarda ahí se pierde en cuanto
        // cy.visit() navega a http://localhost:8080. App.requireAuth() no
        // encuentra token y redirige a login.html. Seteamos la sesión con
        // onBeforeLoad, que corre en el ORIGEN correcto y ANTES de que
        // admin_catalogos.html ejecute su propio <script> (el que llama a
        // App.requireAuth()).
        cy.visit('/admin_catalogos.html', {
            onBeforeLoad(win) {
                win.localStorage.setItem('token', 'token-fake-sesion');
                win.localStorage.setItem('uid', '1');
                win.localStorage.setItem('nombre', 'Edy Ramírez');
                win.localStorage.setItem('rol', 'Administrador');
            },
        });
        cy.wait(['@sedes', '@especialidades', '@laboratorios', '@roles']);
        cy.get('#sel-catalogo').select('sucursal-especialidad');
        cy.wait('@listado');
    };

    describe('Comportamiento confirmado como correcto (ya no es gap)', () => {
        it('GAP #1: la asignación vive dentro de admin_catalogos.html, no en rutas propias /branch-specialty', () => {
            abrirCatalogoConDatos();
            cy.location('pathname').should('include', 'admin_catalogos.html');
            cy.location('pathname').should('not.include', 'branch-specialty');
        });

        it('la tabla muestra las columnas ID | Sede | Especialidad | Estado | Acciones', () => {
            abrirCatalogoConDatos();
            cy.get('#tb-head tr th').then(($ths) => {
                const textos = [...$ths].map(th => th.textContent.trim());
                expect(textos).to.deep.equal(['ID', 'Sede', 'Especialidad', 'Estado', 'Acciones']);
            });
            cy.get('#tb-body tr').first().within(() => {
                cy.get('td').eq(1).should('contain.text', 'Sede Central');
                cy.get('td').eq(2).should('contain.text', 'Medicina General');
            });
        });

        it('FA01: sin asignaciones, muestra un mensaje genérico de tabla vacía', () => {
            abrirCatalogoConDatos([]);
            cy.get('#tb-body').should('contain.text', 'No se encontraron registros.');
        });

        it('el formulario muestra los dropdowns de Sede y Especialidad, precargados solo con registros activos', () => {
            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').click();
            cy.get('#cat-suc option').should('have.length', sedes.length + 1); // placeholder + N
            cy.get('#cat-esp option').should('have.length', especialidades.length + 1);
        });
    });

    describe('Fix GAP #2: textos ahora calzan con el documento', () => {
        it('el botón para abrir el formulario ahora dice "Asignar Especialidad"', () => {
            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').should('exist');
            cy.contains('button', 'Crear / Asignar Registro').should('not.exist');
        });

        it('el botón de guardado ahora dice "Asignar"', () => {
            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').click();
            cy.get('#modal-container').should('be.visible');
            cy.get('#btn-guardar').should('have.text', 'Asignar');
        });

        it('otros catálogos NO se ven afectados por el cambio de texto (sigue diciendo lo de siempre)', () => {
            abrirCatalogoConDatos();
            cy.get('#sel-catalogo').select('sucursales');
            cy.contains('button', 'Crear / Asignar Registro').should('exist');
            cy.contains('button', 'Crear / Asignar Registro').click();
            cy.get('#btn-guardar').should('have.text', 'Confirmar Guardado');
        });

        it('el botón de la fila ahora dice "Eliminar", no "Remover"', () => {
            abrirCatalogoConDatos();
            cy.get('#tb-body tr').first().within(() => {
                cy.contains('button', 'Eliminar').should('exist');
                cy.contains('button', 'Remover').should('not.exist');
            });
        });

        it('el diálogo de confirmación ahora dice "¿Confirmar eliminación?"', () => {
            abrirCatalogoConDatos();
            cy.get('#tb-body tr').first().contains('button', 'Eliminar').click();
            cy.get('#app-confirm-titulo').should('have.text', '¿Confirmar eliminación?');
        });
    });

    describe('Fix GAP #3: se respeta el mensaje de éxito del backend', () => {
        it('al asignar, se muestra el texto exacto que devuelve el backend, no un texto genérico', () => {
            cy.intercept('POST', '/api/sucursal-especialidad', {
                statusCode: 201,
                body: {
                    mensaje: 'Especialidad asignada a la sede correctamente',
                    datos: { id: 501, sucursalId: 2, sucursalNombre: 'Sede Zona 10', especialidadId: 11, especialidadNombre: 'Pediatría', activo: true },
                },
            }).as('asignar');

            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').click();
            cy.get('#cat-suc').select('Sede Zona 10');
            cy.get('#cat-esp').select('Pediatría');
            cy.intercept('GET', '/api/sucursal-especialidad', [asignacionExistente]).as('recarga');
            cy.get('#btn-guardar').click();
            cy.wait('@asignar');

            cy.get('#msg').should('contain.text', 'Especialidad asignada a la sede correctamente');
            cy.get('#msg').should('not.contain.text', "El registro 'Asignación' ha sido creado exitosamente.");
        });

        it('en otros catálogos, que no devuelven "mensaje", se sigue mostrando el texto genérico de siempre', () => {
            cy.intercept('POST', '/api/sucursales', { statusCode: 201, body: { id: 3, nombre: 'Sede Nueva', activo: true } }).as('crearSede');
            abrirCatalogoConDatos();
            cy.get('#sel-catalogo').select('sucursales');
            cy.contains('button', 'Crear / Asignar Registro').click();
            cy.get('#cat-nombre').type('Sede Nueva');
            cy.intercept('GET', '/api/sucursales', sedes).as('recargaSedes');
            cy.get('#btn-guardar').click();
            cy.wait('@crearSede');
            cy.get('#msg').should('contain.text', "El registro 'Sede Nueva' ha sido creado exitosamente.");
        });
    });

    describe('Fix GAP #5: sin validación nativa del navegador', () => {
        it('los <select> de Sede y Especialidad ya no tienen el atributo required', () => {
            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').click();
            cy.get('#cat-suc').should('not.have.attr', 'required');
            cy.get('#cat-esp').should('not.have.attr', 'required');
        });

        it('FA03: al enviar el formulario sin seleccionar Sede, el mensaje del backend se muestra normal (sin bloqueo del navegador)', () => {
            cy.intercept('POST', '/api/sucursal-especialidad', {
                statusCode: 400,
                body: { sucursalId: 'Debe seleccionar una sede.' },
            }).as('asignarInvalido');

            abrirCatalogoConDatos();
            cy.contains('button', 'Asignar Especialidad').click();
            cy.get('#cat-esp').select('Medicina General');
            cy.get('#btn-guardar').click(); // #cat-suc queda sin seleccionar, y el submit SÍ ocurre
            cy.wait('@asignarInvalido');
            cy.get('#msg').should('contain.text', 'Debe seleccionar una sede.');
        });
    });

    it('FA05: asignación duplicada muestra el mensaje de error real del backend', () => {
        cy.intercept('POST', '/api/sucursal-especialidad', {
            statusCode: 409,
            body: { error: 'Esta combinación de sede y especialidad ya existe en el sistema.' },
        }).as('asignarDuplicado');

        abrirCatalogoConDatos();
        cy.contains('button', 'Asignar Especialidad').click();
        cy.get('#cat-suc').select('Sede Central');
        cy.get('#cat-esp').select('Medicina General');
        cy.get('#btn-guardar').click();
        cy.wait('@asignarDuplicado');
        cy.get('#msg').should('contain.text', 'Esta combinación de sede y especialidad ya existe en el sistema.');
    });

    // GAP #4 (RN-CU12-01, "índice único") — YA CORREGIDO a nivel de backend:
    //  - SucursalEspecialidad.java: @UniqueConstraint real en
    //    (sucursal_id, especialidad_id).
    //  - SucursalEspecialidadServiceImpl.asignar(): reutiliza la fila existente
    //    (findBySucursalIdAndEspecialidadId) en vez de insertar una nueva cada
    //    vez, y captura DataIntegrityViolationException para devolver el mismo
    //    DuplicateResourceException de siempre si la condición de carrera
    //    ocurre en el flush().
    // Sigue en SKIP aquí porque un e2e mockeado con cy.intercept no golpea una
    // BD real y no puede reproducir dos inserts concurrentes compitiendo por
    // el mismo constraint. Se requiere una prueba de integración (por ejemplo,
    // dos llamadas HTTP concurrentes reales contra el backend con Postgres de
    // prueba) para verificar esto de punta a punta.
    it.skip('GAP #4 (requiere prueba de integración, no e2e): dos asignaciones concurrentes de la misma combinación no crean un duplicado', () => {
        // Intencionalmente vacío — ver nota arriba.
    });
});