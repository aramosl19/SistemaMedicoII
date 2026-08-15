// Ubicación en tu proyecto: qa/cypress/e2e/02_CU02_registro_paciente.cy.js
//
// Escrito contra "2_CU_Registro_de_usuarios_externos.docx" y
// "Reglas_de_Negocio_Consolidadas.docx" (RN-CU02-01 a 06, RN-GLOBAL-001,
// RN-GLOBAL-002).
//
// NOTA IMPORTANTE sobre qué prueba cada test de validación (nombre, NIT,
// teléfono, correo, usuario, contraseña, seguro médico): registro.html usa
// atributos HTML5 (required / pattern / minlength / maxlength) que
// bloquean el envío del formulario EN EL NAVEGADOR antes de que exista
// cualquier llamada de red. Por eso, en todos estos casos, lo único que se
// puede comprobar desde Cypress es que el campo específico queda marcado
// en rojo y aparece el mensaje genérico "Revise los campos marcados en
// rojo antes de continuar." — el mensaje EXACTO de cada regla de negocio
// (incluyendo los conteos dinámicos tipo "Usted ingresó 6 caracteres.")
// nunca llega a mostrarse en pantalla porque el backend nunca se llama.
// Ese contrato exacto se verifica del lado de Spring Boot, en
// PortalControllerTest.java (MockMvc contra el @Valid real del DTO), no
// aquí. Dejarlo simulado aquí con un mock de texto distinto sería
// maquillar el test.
//
// NOTA sobre el DPI (RN-GLOBAL-001): en registro.html el campo #dpi es
// readonly (se precarga por query param desde CU-00). No hay forma de
// escribir un DPI inválido en ESTE formulario para probarlo — esa
// validación ya se cubre en el spec de CU-00 (donde el usuario sí lo
// escribe) y en PortalControllerTest (mensajes exactos de longitud vs.
// solo-numérico).
//
// NOTA sobre el username "muy largo" (RN-CU02-05): el campo #username
// tiene maxlength="9" en el HTML, así que un usuario real -tecleando o
// pegando- nunca puede meter más de 9 caracteres: el navegador lo trunca
// antes de que exista algo que validar. El caso "más de 9 caracteres" solo
// puede darse si alguien se salta esa restricción del lado del cliente
// (ej. editando el DOM desde "Inspeccionar elemento", o interceptando la
// petición). Por eso ese test setea el value directamente con .invoke('val', ...)
// en vez de .type(), simulando exactamente ese escenario, y verifica que
// aun así la validación (forzarValidezLongitud() en registro.html) lo
// detecta y marca el campo en rojo.

describe('CU-02 - Registro de paciente externo', () => {
    const dpiPrecargado = '1234567890123';

    const llenarFormularioValido = () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('ContraseñaSegura123');
    };

    beforeEach(() => {
        cy.visit(`/registro.html?dpi=${dpiPrecargado}`);
    });

    it('precarga el DPI recibido por la URL y lo deja de solo lectura', () => {
        cy.get('#dpi').should('have.value', dpiPrecargado).and('have.attr', 'readonly');
    });

    it('si se accede sin el parámetro dpi, redirige al portal principal', () => {
        cy.visit('/registro.html');
        cy.location('pathname', { timeout: 6000 }).should('include', 'index.html');
    });

    it('Camino feliz: con datos válidos, registra al paciente, envía correo de bienvenida y redirige al login', () => {
        cy.intercept('POST', '/api/portal/registro', {
            statusCode: 201,
            body: { id: 5, nombreUsuario: 'cramirez', rolNombre: 'Paciente' },
        }).as('registro');

        llenarFormularioValido();
        cy.get('#btn-reg').click();
        cy.wait('@registro');

        cy.get('#msg-reg').should('contain.text', '¡Registro exitoso!');
        cy.location('pathname', { timeout: 6000 }).should('include', 'login.html');
        // Nota: el envío del correo de bienvenida es responsabilidad del backend
        // y no se puede verificar de forma confiable desde un test E2E de
        // frontend; esa parte de la postcondición debe cubrirse con un test
        // unitario/MockMvc del lado de Spring Boot.
    });

    // RN-CU02-03: el número de afiliado del seguro médico es opcional, pero
    // si el Usuario Externo SÍ lo tiene, debe poder ingresarlo y quedar
    // incluido en el registro.
    it('RN-CU02-03: con número de afiliado del seguro médico ingresado, lo envía en el registro', () => {
        cy.intercept('POST', '/api/portal/registro', {
            statusCode: 201,
            body: { id: 5, nombreUsuario: 'cramirez', rolNombre: 'Paciente' },
        }).as('registro');

        llenarFormularioValido();
        cy.get('#seguro').type('AFIL-004521');
        cy.get('#btn-reg').click();
        cy.wait('@registro').its('request.body.numeroSeguro').should('eq', 'AFIL-004521');

        cy.get('#msg-reg').should('contain.text', '¡Registro exitoso!');
    });

    // RN-CU02-03: si SÍ se ingresa, debe respetar 5-50 caracteres.
    it('RN-CU02-03: número de afiliado fuera de rango (menos de 5 caracteres) marca el campo en rojo', () => {
        llenarFormularioValido();
        cy.get('#seguro').type('ab1'); // 3 caracteres

        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#seguro').should('have.class', 'is-invalid');
    });

    it('FA01: "Volver al portal" regresa al portal principal', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');

        cy.on('window:confirm', () => true);
        cy.contains('a', 'Volver al portal').click();

        cy.location('pathname', { timeout: 6000 }).should('include', 'index.html');
    });

    it('FA01b: si el usuario cancela la confirmación, permanece en el formulario y no pierde los datos', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');

        cy.on('window:confirm', () => false);
        cy.contains('a', 'Volver al portal').click();

        cy.location('pathname').should('include', 'registro.html');
        cy.get('#nombre').should('have.value', 'Carlos Iván Ramírez López');
    });

    it('FA02: muestra el error del backend si el DPI ya está registrado', () => {
        cy.intercept('POST', '/api/portal/registro', {
            statusCode: 409,
            body: { error: 'Ya existe una cuenta registrada con este número de DPI. Si ya tiene cuenta, inicie sesión.' },
        }).as('registro');

        llenarFormularioValido();
        cy.get('#btn-reg').click();
        cy.wait('@registro');

        cy.get('#msg-reg').should('contain.text', 'Ya existe una cuenta registrada con este número de DPI');
    });

    it('FA03: muestra el error del backend si el correo ya está registrado', () => {
        cy.intercept('POST', '/api/portal/registro', {
            statusCode: 409,
            body: { error: 'Ya existe una cuenta registrada con este correo electrónico.' },
        }).as('registro');

        llenarFormularioValido();
        cy.get('#btn-reg').click();
        cy.wait('@registro');

        cy.get('#msg-reg').should('contain.text', 'Ya existe una cuenta registrada con este correo');
    });

    it('FA04: marca en rojo los campos obligatorios vacíos y no envía el formulario', () => {
        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#nombre').should('have.class', 'is-invalid');
    });

    // RN-CU02-01: nombre completo, 10-100 caracteres (bloqueo cliente vía minlength).
    it('RN-CU02-01: nombre completo con menos de 10 caracteres marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos'); // 6 caracteres
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#nombre').should('have.class', 'is-invalid');
    });

    // RN-GLOBAL-002: NIT, 8-9 caracteres alfanuméricos (bloqueo cliente vía minlength/pattern).
    it('RN-GLOBAL-002: NIT con menos de 8 caracteres marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('1234567'); // 7 caracteres
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#nit').should('have.class', 'is-invalid');
    });

    // RN-CU02-02: teléfono, exactamente 8 dígitos (bloqueo cliente vía pattern).
    it('RN-CU02-02: teléfono con menos de 8 dígitos marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('5551234'); // 7 dígitos
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#telefono').should('have.class', 'is-invalid');
    });

    // RN-CU02-04: correo, formato válido (bloqueo cliente vía type="email").
    it('RN-CU02-04: correo con formato inválido marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('correo-invalido');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
        cy.get('#correo').should('have.class', 'is-invalid');
    });

    // RN-CU02-05: usuario, 8-9 caracteres. Se separan los dos casos (muy
    // corto / muy largo) porque el documento define un mensaje DISTINTO
    // para cada uno (verificado por separado en PortalControllerTest); acá
    // solo se comprueba que ambos casos bloquean el envío.
    it('RN-CU02-05: nombre de usuario con menos de 8 caracteres marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('abc'); // muy corto
        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#username').should('have.class', 'is-invalid');
        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    it('RN-CU02-05: si se fuerza un valor de más de 9 caracteres saltándose maxlength (ej. editando el DOM), la validación igual lo detecta y marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');

        // Simula editar el valor desde "Inspeccionar elemento": se setea el
        // value directamente, saltándose el maxlength="9" que el navegador
        // aplica solo cuando el usuario teclea de verdad.
        cy.get('#username').invoke('val', 'cramirezlopez').trigger('input');

        cy.get('#password').type('ContraseñaSegura123');

        cy.get('#btn-reg').click();

        cy.get('#username').should('have.class', 'is-invalid');
        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    it('RN-CU02-06: contraseña con menos de 12 caracteres marca el campo en rojo', () => {
        cy.get('#nombre').type('Carlos Iván Ramírez López');
        cy.get('#nit').type('12345678');
        cy.get('#telefono').type('55512345');
        cy.get('#correo').type('carlos@correo.com');
        cy.get('#username').type('cramirez');
        cy.get('#password').type('corta1'); // menos de 12 caracteres

        cy.get('#btn-reg').click();

        cy.get('#password').should('have.class', 'is-invalid');
        cy.get('#msg-reg').should('contain.text', 'Revise los campos marcados en rojo');
    });
});