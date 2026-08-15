describe('CU-00 - Portal público: verificación de DPI', () => {
  beforeEach(() => {
    cy.visit('/index.html');
  });

  it('Camino feliz: DPI válido y registrado como paciente redirige a inicio de sesión', () => {
    cy.intercept('POST', '/api/portal/verificar-dpi', {
      statusCode: 200,
      body: { registrado: true, rol: 'Paciente', nombreCompleto: 'Ana López' },
    }).as('verificarDpi');

    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#dpi').type('1234567890123');
    cy.get('#contador').should('have.text', '13');
    cy.get('#btn-verificar').click();
    cy.wait('@verificarDpi');

    cy.get('#msg-dpi').should('contain.text', 'Bienvenido(a) nuevamente, Ana López');
    cy.location('pathname', { timeout: 6000 }).should('include', 'login.html');
  });

  it('FA01: con un DPI incompleto, muestra el contador en tiempo real y el error de formato, corrige el dato y puede continuar la verificación', () => {
    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#dpi').type('123456');
    cy.get('#contador').should('have.text', '6');
    cy.get('#btn-verificar').click();
    cy.get('#msg-dpi')
        .should('be.visible')
        .and('contain.text', 'El DPI debe contener exactamente 13 dígitos. Usted ingresó 6 dígitos.');

    cy.intercept('POST', '/api/portal/verificar-dpi', {
      statusCode: 200,
      body: { registrado: false },
    }).as('verificarDpi');

    cy.get('#dpi').type('7890123');
    cy.get('#contador').should('have.text', '13');
    cy.get('#btn-verificar').click();
    cy.wait('@verificarDpi');
    cy.get('#msg-dpi').should('contain.text', 'No se encontró un registro asociado a este DPI');
  });

  it('FA02: al cancelar, cierra el modal y regresa a la página principal', () => {
    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#modal-dpi').should('not.have.class', 'd-none');

    cy.get('#modal-dpi').within(() => {
      cy.contains('button', 'Cancelar').click();
    });

    cy.get('#modal-dpi').should('have.class', 'd-none');
  });

  it('FA03: si el DPI no está registrado, avisa y redirige al formulario de registro con el DPI precargado', () => {
    cy.intercept('POST', '/api/portal/verificar-dpi', {
      statusCode: 200,
      body: { registrado: false },
    }).as('verificarDpi');

    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#dpi').type('1234567890123');
    cy.get('#btn-verificar').click();
    cy.wait('@verificarDpi');

    cy.get('#msg-dpi').should('contain.text', 'No se encontró un registro asociado a este DPI');
    cy.location('pathname', { timeout: 6000 }).should('include', 'registro.html');
    cy.location('search').should('include', 'dpi=1234567890123');
  });

  it('FA04: si el DPI pertenece a un usuario interno (no paciente), pide contactar recepción y no redirige', () => {
    cy.intercept('POST', '/api/portal/verificar-dpi', {
      statusCode: 200,
      body: { registrado: true, rol: 'Enfermero', nombreCompleto: 'Juan Pérez' },
    }).as('verificarDpi');

    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#dpi').type('1234567890123');
    cy.get('#btn-verificar').click();
    cy.wait('@verificarDpi');

    cy.get('#msg-dpi').should('contain.text', 'Este DPI pertenece a un usuario del sistema interno');
    cy.location('pathname').should('include', 'index.html');
  });

  it('FA05: si falla la conexión con el servidor, muestra el mensaje de error de red', () => {
    cy.intercept('POST', '/api/portal/verificar-dpi', { forceNetworkError: true }).as('verificarDpiCaida');

    cy.contains('button', '¿Paciente nuevo?').click();
    cy.get('#dpi').type('1234567890123');
    cy.get('#btn-verificar').click();

    cy.get('#msg-dpi').should('contain.text', 'No se pudo conectar con el servidor');
  });
});
