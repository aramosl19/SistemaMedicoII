// Comando custom para simular una sesión ya iniciada, sin pasar por el
// formulario de login. Las claves usadas (token, uid, nombre, rol) son
// exactamente las que login.html guarda en localStorage al autenticarse
// (ver js/app.js y el script de login.html).
Cypress.Commands.add('simularSesion', (datos = {}) => {
    const sesion = {
        token: 'token-fake-sesion',
        uid: 1,
        nombre: 'Usuario Demo',
        rol: 'Paciente',
        ...datos,
    };

    cy.window().then((win) => {
        win.localStorage.setItem('token', sesion.token);
        win.localStorage.setItem('uid', sesion.uid);
        win.localStorage.setItem('nombre', sesion.nombre);
        win.localStorage.setItem('rol', sesion.rol);
    });
});