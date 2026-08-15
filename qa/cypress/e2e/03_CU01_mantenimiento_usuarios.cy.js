describe('CU-01 - Mantenimiento de Usuarios', () => {
  const roles = [
    { id: 1, nombre: 'Administrador', activo: true },
    { id: 2, nombre: 'Paciente', activo: true },
    { id: 3, nombre: 'Médico', activo: true },
  ];
  const sucursales = [{ id: 1, nombre: 'Sede Central', activo: true }];
  const especialidades = [{ id: 1, nombre: 'Medicina General', activo: true }];

  const usuarioBase = {
    id: 10,
    nombreCompleto: 'Ana López Pérez',
    dpi: '1234567890123',
    correo: 'ana@correo.com',
    nombreUsuario: 'alopez01',
    rolNombre: 'Paciente',
    rolId: 2,
    sucursalNombre: null,
    activo: true,
  };

  const paginaCon = (usuarios) => ({
    content: usuarios,
    number: 0,
    totalPages: 1,
    totalElements: usuarios.length,
  });

  describe('Listado de Usuarios (admin_usuarios.html)', () => {
    beforeEach(() => {
      cy.simularSesion({ rol: 'Administrador', nombre: 'Edy Ramírez' });
      cy.intercept('GET', '/api/roles', roles).as('roles');
      cy.intercept('GET', '/api/sucursales', sucursales).as('sucursales');
      cy.intercept('GET', '/api/especialidades', especialidades).as('especialidades');
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('buscarInicial');
      cy.visit('/admin_usuarios.html');
      cy.wait(['@roles', '@sucursales', '@especialidades', '@buscarInicial']);
    });

    it('Camino feliz: busca usuarios por un criterio y muestra los resultados en la tabla paginada', () => {
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('buscarFiltrado');

      cy.get('#filtroCampo').select('nombre');
      cy.get('#filtroValor').type('Ana');
      cy.contains('button', 'Buscar').click();
      cy.wait('@buscarFiltrado');

      cy.get('#tb-usuarios tr').should('have.length', 1);
      cy.get('#tb-usuarios').should('contain.text', 'Ana López Pérez');
      cy.get('#page-info').should('contain.text', '1 registro(s) en total');
    });

    it('Camino feliz (navegación completa): entra desde el menú "Usuarios" → "Listar Usuarios" y usa el selector de elementos por página', () => {
      // Paso 2 del documento: el menú despliega "Listar Usuarios" / "Crear
      // Usuario". Como ya estamos parados en admin_usuarios.html, el submenú
      // se auto-expande y marca "Listar Usuarios" como la opción activa.
      cy.get('#toggle-usuarios').should('have.class', 'open').and('contain.text', 'Usuarios');
      cy.get('#submenu-usuarios').should('have.class', 'open');
      cy.get('#submenu-usuarios').contains('a', 'Listar Usuarios').should('have.class', 'active');
      cy.get('#submenu-usuarios').contains('a', 'Crear Usuario').should('be.visible');

      // Paso 7 del documento: "Elementos Por Página" (10/25/50).
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('buscarPageSize');
      cy.get('#pageSize').select('50');
      cy.wait('@buscarPageSize').its('request.url').should('include', 'size=50');

      // El submenú también permite ir a "Crear Usuario" como opción
      // independiente (paso 2 del documento).
      cy.get('#submenu-usuarios').contains('a', 'Crear Usuario').click();
      cy.url().should('include', '/crear_usuario.html');
      cy.get('#u-nombre').should('be.visible');

      // NOTA: el paso 8 del documento describe un menú de acciones "⋮" por
      // fila. La app real usa botones directos "Editar"/"Eliminar" (ver
      // FA04/FA05 más abajo) — no existe un menú "⋮" todavía, así que no se
      // prueba aquí.
    });

    it('FA02: limpiar búsqueda (sin criterio) muestra la tabla completa sin filtro', () => {
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('buscarSinFiltro');

      cy.get('#filtroValor').clear();
      cy.contains('button', 'Buscar').click();
      cy.wait('@buscarSinFiltro');

      cy.get('#tb-usuarios').should('contain.text', 'Ana López Pérez');
    });

    it('FA03: no se encontró información con los filtros dados', () => {
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([])).as('buscarVacio');

      cy.get('#filtroValor').type('xxxxxxxx');
      cy.contains('button', 'Buscar').click();
      cy.wait('@buscarVacio');

      // Texto exacto según el documento del CU-01 (contra documento, a propósito).
      // NOTA: este test va a FALLAR hasta que se corrija el mensaje real de la
      // app, que actualmente dice "No se encontraron usuarios." en vez de
      // "No se encontraron datos usuarios". Es un bug de texto en la UI, no del
      // test — pendiente de que el ingeniero lo corrija.
      cy.get('#tb-usuarios').should('contain.text', 'No se encontraron datos usuarios');
    });

    it('FA04: edita un usuario existente precargando sus datos', () => {
      cy.get('#tb-usuarios').contains('button', 'Editar').click();

      cy.get('#form-title').should('contain.text', 'Modificar Usuario');
      cy.get('#u-nombre').should('have.value', 'Ana López Pérez');
      cy.get('#u-nombre').clear().type('Ana López Pérez de Castillo');

      cy.intercept('PUT', '/api/usuarios/10', { statusCode: 200, body: {} }).as('actualizar');
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('recarga');

      cy.get('#modal-user').within(() => {
        cy.contains('button', 'Guardar Información').click();
      });
      cy.wait('@actualizar');

      cy.get('#msg').should('contain.text', 'Usuario actualizado correctamente');

      // PENDIENTE (sin disfrazar, a propósito no se agrega aquí todavía):
      // el documento pide que FA04 precargue TAMBIÉN Correo, Nombre de
      // Usuario, DPI, Teléfono, Rol, NIT, Número de Seguro, Sucursal,
      // Especialidad y Estado — este test solo verifica #u-nombre. Tampoco
      // hay test para el campo "Nueva Contraseña (opcional)" en edición.
      // Se deja fuera junto con la discrepancia modal-vs-pantalla que se
      // va a resolver el sábado, ya que ambos puntos caen dentro de la
      // misma conversación pendiente sobre cómo debe verse FA04.
    });

    it('FA04 FA07: cancelar en el modal de edición no guarda los cambios', () => {
      cy.get('#tb-usuarios').contains('button', 'Editar').click();
      cy.get('#u-nombre').clear().type('Nombre que no se debe guardar');

      cy.get('#modal-user').within(() => {
        cy.contains('button', 'Cancelar').click();
      });

      cy.get('#modal-user').should('not.be.visible');
      cy.get('#tb-usuarios').should('contain.text', 'Ana López Pérez');
      cy.get('#tb-usuarios').should('not.contain.text', 'Nombre que no se debe guardar');
    });

    it('FA05: elimina un usuario tras confirmar en el modal', () => {
      cy.get('#tb-usuarios').contains('button', 'Eliminar').click();

      cy.get('#modal-confirm-delete').should('not.have.class', 'd-none');
      cy.get('#modal-confirm-delete').should('contain.text', 'Confirmar eliminación');
      cy.get('#modal-confirm-delete').should('contain.text', 'Esta acción no se puede deshacer');
      cy.get('#confirm-delete-nombre').should('contain.text', 'Ana López Pérez');

      cy.intercept('DELETE', '/api/usuarios/10', { statusCode: 200, body: {} }).as('eliminar');
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('recarga');

      cy.get('#modal-confirm-delete').within(() => {
        cy.contains('button', 'Eliminar').click();
      });
      cy.wait('@eliminar');

      cy.get('#msg').should('contain.text', 'ha sido eliminado correctamente');
    });

    it('FA07: cancelar en el modal de confirmación de eliminación no hace cambios', () => {
      cy.get('#tb-usuarios').contains('button', 'Eliminar').click();
      cy.get('#modal-confirm-delete').within(() => {
        cy.contains('button', 'Cancelar').click();
      });
      cy.get('#modal-confirm-delete').should('have.class', 'd-none');
      cy.get('#tb-usuarios').should('contain.text', 'Ana López Pérez');
    });
  });

  describe('Crear Usuario (crear_usuario.html — pantalla independiente)', () => {
    beforeEach(() => {
      cy.simularSesion({ rol: 'Administrador', nombre: 'Edy Ramírez' });
      cy.intercept('GET', '/api/roles', roles).as('roles');
      cy.intercept('GET', '/api/sucursales', sucursales).as('sucursales');
      cy.intercept('GET', '/api/especialidades', especialidades).as('especialidades');
      cy.visit('/crear_usuario.html');
      cy.wait(['@roles', '@sucursales', '@especialidades']);
    });

    it('FA01: crea un nuevo usuario con datos válidos y regresa al listado', () => {
      cy.get('#u-nombre').type('Carlos Iván Ramírez López');
      cy.get('#u-correo').type('carlos@correo.com');
      cy.get('#u-username').type('cramirez1');
      cy.get('#u-pass').type('ContraseñaSegura123');
      cy.get('#u-rol').select('Paciente');
      cy.get('#u-suc').select('Sede Central');

      cy.intercept('POST', '/api/usuarios', { statusCode: 201, body: { id: 99 } }).as('crear');
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('recarga');

      cy.contains('button', 'Registrar Usuario').click();
      cy.wait('@crear');

      cy.url().should('include', '/admin_usuarios.html');
      cy.wait('@recarga');
      cy.get('#msg').should('contain.text', 'Usuario creado correctamente');
    });

    it('FA01b: crea un usuario con rol Médico incluyendo Especialidad y campos opcionales', () => {
      cy.get('#u-nombre').type('Laura Méndez Ochoa');
      cy.get('#u-dpi').type('1122334455667');
      cy.get('#u-tel').type('55501234');
      cy.get('#u-username').type('lmendez1');
      cy.get('#u-pass').type('ContraseñaSegura123');
      cy.get('#u-nit').type('1234567');
      cy.get('#u-seguro').type('SEG-000123');
      cy.get('#u-rol').select('Médico');

      // El campo Especialidad solo aparece para el rol Médico.
      cy.get('#div-esp').should('be.visible');
      cy.get('#u-esp').select('Medicina General');
      cy.get('#u-suc').select('Sede Central');
      cy.get('#u-correo').type('laura@correo.com');

      cy.intercept('POST', '/api/usuarios', (req) => {
        expect(req.body).to.include({
          dpi: '1122334455667',
          telefono: '55501234',
          nit: '1234567',
          numeroSeguro: 'SEG-000123',
          activo: true,
        });
        expect(req.body.especialidadId).to.exist;
        req.reply({ statusCode: 201, body: { id: 100 } });
      }).as('crearMedico');
      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('recarga');

      cy.contains('button', 'Registrar Usuario').click();
      cy.wait('@crearMedico');

      cy.url().should('include', '/admin_usuarios.html');
      cy.wait('@recarga');
      cy.get('#msg').should('contain.text', 'Usuario creado correctamente');
    });

    it('FA06: no envía el formulario si hay campos obligatorios vacíos', () => {
      cy.contains('button', 'Registrar Usuario').click();

      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
      cy.get('#u-nombre').should('have.class', 'is-invalid');
    });

    it('FA06b: correo con formato inválido muestra error de validación específico', () => {
      // Nota de orden: se llena #u-correo al final, ya que mientras tiene
      // un valor inválido, el navegador marca el formulario como inválido
      // antes de intentar el submit.
      cy.get('#u-nombre').type('Carlos Iván Ramírez López');
      cy.get('#u-username').type('cramirez1');
      cy.get('#u-pass').type('ContraseñaSegura123');
      cy.get('#u-rol').select('Paciente');
      cy.get('#u-suc').select('Sede Central');
      cy.get('#u-correo').type('correo-sin-arroba');

      cy.contains('button', 'Registrar Usuario').click();

      cy.get('#u-correo').should('have.class', 'is-invalid');
      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    // RN-CU01-05: Nombre de Usuario. El documento lo lista sin marcarlo
    // "opcional" (a diferencia de DPI, Teléfono, NIT, Seguro, Sucursal y
    // Especialidad, que sí lo dicen explícitamente), así que se trata como
    // obligatorio. Solo se prueba el caso vacío; no se conocen reglas de
    // longitud/patrón específicas para este campo en CU-01 (a diferencia
    // de CU-02), así que no se inventan.
    it('RN-CU01-05: nombre de usuario vacío marca el campo en rojo', () => {
      cy.get('#u-nombre').type('Carlos Iván Ramírez López');
      cy.get('#u-correo').type('carlos@correo.com');
      cy.get('#u-pass').type('ContraseñaSegura123');
      cy.get('#u-rol').select('Paciente');
      cy.get('#u-suc').select('Sede Central');

      cy.contains('button', 'Registrar Usuario').click();

      cy.get('#u-username').should('have.class', 'is-invalid');
      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    // RN-CU01-06: Contraseña, también obligatoria (no aparece marcada como
    // "opcional" en el documento). Mismo criterio: solo caso vacío.
    it('RN-CU01-06: contraseña vacía marca el campo en rojo', () => {
      cy.get('#u-nombre').type('Carlos Iván Ramírez López');
      cy.get('#u-correo').type('carlos@correo.com');
      cy.get('#u-username').type('cramirez1');
      cy.get('#u-rol').select('Paciente');
      cy.get('#u-suc').select('Sede Central');

      cy.contains('button', 'Registrar Usuario').click();

      cy.get('#u-pass').should('have.class', 'is-invalid');
      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    // RN-CU01-09: Rol, obligatorio (tampoco marcado "opcional" en el
    // documento, a diferencia de Sucursal y Especialidad).
    it('RN-CU01-09: no seleccionar rol marca el campo en rojo', () => {
      cy.get('#u-nombre').type('Carlos Iván Ramírez López');
      cy.get('#u-correo').type('carlos@correo.com');
      cy.get('#u-username').type('cramirez1');
      cy.get('#u-pass').type('ContraseñaSegura123');
      cy.get('#u-suc').select('Sede Central');

      cy.contains('button', 'Registrar Usuario').click();

      cy.get('#u-rol').should('have.class', 'is-invalid');
      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    it('FA07: cancelar en la pantalla de creación regresa al listado sin guardar', () => {
      cy.get('#u-nombre').type('Usuario que no se debe guardar');

      cy.intercept('GET', '/api/usuarios/buscar*', paginaCon([usuarioBase])).as('recarga');
      cy.contains('a', 'Cancelar').click();

      cy.url().should('include', '/admin_usuarios.html');
      cy.url().should('not.include', 'msg=creado');
      cy.wait('@recarga');
    });
  });
});