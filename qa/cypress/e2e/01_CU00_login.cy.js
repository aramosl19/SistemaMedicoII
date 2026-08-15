describe('CU-00 - Inicio de sesión', () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    cy.visit('/login.html');
  });

  it('Camino feliz: con credenciales válidas, guarda la sesión y redirige al dashboard', () => {
    cy.intercept('POST', '/api/portal/login', {
      statusCode: 200,
      body: { id: 1, nombreCompleto: 'Ana López', nombreUsuario: 'alopez01', rol: 'Paciente', token: 'token-jwt-abc' },
    }).as('login');

    cy.get('#username').type('alopez01');
    cy.get('#password').type('Clave123456');
    cy.get('#btn-submit').click();
    cy.wait('@login');

    cy.location('pathname', { timeout: 6000 }).should('include', 'dashboard.html');
    cy.window().its('localStorage.token').should('eq', 'token-jwt-abc');
  });

  it('si ya existe una sesión activa, redirige directo al dashboard sin mostrar el formulario', () => {
    cy.simularSesion({ token: 'token-existente' });
    cy.visit('/login.html');
    cy.location('pathname', { timeout: 6000 }).should('include', 'dashboard.html');
  });

  it('FA06: credenciales incorrectas muestran el mensaje de error con intentos restantes (RN-CU00-02)', () => {
    cy.intercept('POST', '/api/portal/login', {
      statusCode: 401,
      body: { error: 'Usuario o contraseña incorrectos. Intentos restantes: 3.' },
    }).as('login');

    cy.get('#username').type('alopez01');
    cy.get('#password').type('claveMala');
    cy.get('#btn-submit').click();
    cy.wait('@login');

    cy.get('#msg-login')
        .should('be.visible')
        .and('contain.text', 'Usuario o contraseña incorrectos. Intentos restantes: 3.');
    cy.location('pathname').should('include', 'login.html');
  });

  it('FA07: al bloquearse la cuenta, deshabilita usuario, contraseña y el botón de ingreso', () => {
    cy.intercept('POST', '/api/portal/login', {
      statusCode: 423,
      body: { error: 'Cuenta bloqueada temporalmente. Intente de nuevo en 15 minutos.' },
    }).as('login');

    cy.get('#username').type('alopez01');
    cy.get('#password').type('claveMala');
    cy.get('#btn-submit').click();
    cy.wait('@login');

    cy.get('#msg-login').should('contain.text', 'Cuenta bloqueada temporalmente');
    cy.get('#username').should('be.disabled');
    cy.get('#password').should('be.disabled');
    cy.get('#btn-submit').should('be.disabled');
  });

  it('FA08: si falla la conexión con el servidor durante el login, muestra el mensaje de error de red', () => {
    cy.intercept('POST', '/api/portal/login', { forceNetworkError: true }).as('loginCaido');

    cy.get('#username').type('alopez01');
    cy.get('#password').type('Clave123456');
    cy.get('#btn-submit').click();

    cy.get('#msg-login').should('contain.text', 'No se pudo conectar con el servidor');
  });

  // FA09 (rol no autorizado) NO está implementado en login.html: el login es
  // compartido para todos los roles internos (Administrador, Médico, Enfermero,
  // etc.) y redirige siempre a dashboard.html sin filtrar por "Paciente".
  // DEJADO EN SKIP A PROPÓSITO: se revisará con el ingeniero el sábado para
  // decidir si login.html debe restringir el acceso por rol o si esta parte
  // del CU-00 debe actualizarse en el documento.
  it.skip('FA09: login con rol distinto de Paciente muestra acceso exclusivo para pacientes', () => {
    cy.intercept('POST', '/api/portal/login', {
      statusCode: 200,
      body: { id: 5, nombreCompleto: 'Juan Pérez', nombreUsuario: 'jperez', rol: 'Enfermero', token: 'token-jwt-xyz' },
    }).as('login');

    cy.get('#username').type('jperez');
    cy.get('#password').type('Clave123456');
    cy.get('#btn-submit').click();
    cy.wait('@login');

    cy.get('#msg-login').should('contain.text', 'Este acceso es exclusivo para pacientes');
    cy.contains('a', 'Acceso Panel Administrativo').should('be.visible');
  });
});
