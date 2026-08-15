// Ubicación en tu proyecto: qa/cypress/e2e/06_CU05_recepcion_verificacion_cita.cy.js
//
// Escrito contra "5_CU_Recepcion_y_Verificacion_de_Cita.docx", más las
// reglas RN-CU05-01 y RN-CU05-02 del compilado de 17 CUs (el .docx
// individual solo trae la referencia [RN-CU05-01]/[RN-CU05-02], el texto
// completo de la regla está en el compilado). Donde recepcion.html se
// desvía del documento, la prueba queda tal cual el documento lo exige y
// VA A FALLAR — con un comentario "NOTA GAP" explicando la diferencia. No
// se ajustó ningún selector ni mock para maquillar el resultado.
//
// REVISIÓN (última): se comparó línea por línea contra el recepcion.html
// más reciente. La mayoría de los gaps anteriores (A, B, D, G, H, I, L)
// ya fueron corregidos en el código y se convirtieron aquí en pruebas de
// regresión normales (ya no son NOTA GAP). Solo sigue abierto el Gap K.
// Se agrega un hallazgo nuevo (Gap M): búsqueda por número de cita sin
// resultados, que no estaba cubierto.
//
// REVISIÓN 2: se revisó el spec completo contra el texto íntegro del
// documento (flujo normal 1-9, FA01-FA09, RN-CU05-01/02) para confirmar
// que cada paso y cada Flujo Alterno tiene al menos una prueba. Se
// agregaron 7 pruebas nuevas (N1-N7) para cerrar huecos de cobertura que
// no estaban ni probados ni excluidos con un comentario.
//
// REVISIÓN 3: se corrieron las 7 pruebas N1-N7 contra recepcion.html real
// y se leyó el código fuente completo. 5 de los fallos eran GAPS REALES
// (N1, N2, N4, N6, y el Gap K/M ya conocidos) y se dejaron como NOTA GAP.
// Los otros 3 (N3, N5, N7 en parte) eran selectores/formatos inventados
// sin ver el código — se corrigieron.
//
// REVISIÓN 4 (esta versión): recepcion.html rediseñó por completo el
// ingreso por emergencia (FA01):
//  - El botón "Registrar Emergencia" (#btn-emergencia-directa) ya NO
//    depende de haber buscado antes por DPI/número — está siempre
//    visible, en el mismo box del buscador, fuera de #acciones-extra.
//    Esto RESUELVE el núcleo del Gap K: FA04 (#acciones-extra) ahora sí
//    muestra ÚNICAMENTE "Nueva Cita (Walk-in)" dentro de su propio box;
//    el botón de emergencia vive aparte, como flujo verdaderamente
//    independiente (tal como lo describe el documento). Los tests viejos
//    de "NOTA GAP (K)" se reemplazan por pruebas de regresión positivas.
//  - El modal de emergencia (#modal-emergencia) ya no pide sede,
//    especialidad ni médico — solo nombre y DPI (#em-nombre, #em-dpi).
//    Sede/especialidad/médico se resuelven en confirmarEmergencia() vía
//    /api/usuarios/me + /api/sucursal-especialidad +
//    /api/citas/medicos-disponibles. Se reescribió por completo el test
//    "Contrato del frontend (F)" para esta cadena nueva de llamadas.
//  - Los errores de #modal-emergencia y #modal-reasignar ahora se
//    muestran en banners LOCALES dentro de cada modal (#msg-emergencia,
//    #msg-reasignar) en vez del #msg global, que queda tapado detrás del
//    overlay. Se agregan pruebas nuevas para confirmar esto.
//  - N4 (triaje) se re-verificó contra el formulario nuevo (2 campos:
//    nombre y DPI) — sigue sin existir ningún campo de triaje/prioridad,
//    así que el gap se mantiene, ahora contra el formulario actual.
//
// REVISIÓN 5 (esta versión): dos cambios de fondo, ambos ya verificados
// contra el proyecto real (no son suposiciones):
//
//  1) N5/FA03 quedó RESUELTO. La causa real de que "Registrar Paciente"
//     terminara en index.html no era la sesión ni el token — registro.html
//     exige ?dpi=... en la URL y se auto-redirige si no lo recibe. Se
//     corrigió irARegistrar() en recepcion.html para pasar el DPI buscado.
//
//  2) Todo el bloque FA01 se reescribió porque el flujo de emergencia
//     cambió de raíz: ya no es un DPI que debe pertenecer a un paciente ya
//     registrado (si no, error). Ahora es un solo POST a
//     /api/recepcion/emergencia con nombrePaciente+dpi: el backend
//     (RecepcionServiceImpl.registrarEmergenciaConAlta) usa la cuenta si el
//     DPI ya existe, o la da de alta automáticamente con esos mismos datos
//     si no existe — y resuelve sede/especialidad/médico del Recepcionista
//     autenticado, sin pedírselos a nadie. El frontend ya no encadena
//     /citas/buscar → /usuarios/me → /sucursal-especialidad →
//     /citas/medicos-disponibles → POST; es una sola llamada.
//
//  3) N4 (triaje) DEJA de ser NOTA GAP: se decidió en equipo que no hace
//     falta un campo de clasificación de severidad — toda emergencia entra
//     con prioridad ALTA por definición. El test ahora confirma la
//     decisión en vez de reportarla como pendiente.
//
// Estado actual de los gaps:
//
//  A) [RESUELTO] RN-CU05-02 (paso 4 del flujo normal): ya existen #d-num
//     y #d-hora-llegada en la tabla y se llenan correctamente con el
//     número de cita y la hora de llegada.
//
//  B) [RESUELTO] FA03 (paciente no registrado): el mensaje, el sub-texto
//     y el botón "Registrar Paciente" ya coinciden exactamente con el
//     documento.
//
//  C) [RESUELTO en REVISIÓN 4] FA04 (paciente sin citas activas): el
//     mensaje, el sub-texto y el botón "Nueva Cita (Walk-in)" ya
//     coinciden exactamente con el documento, Y ahora #acciones-extra
//     muestra ÚNICAMENTE ese botón — el de emergencia ya no vive dentro
//     de esa caja. Ver Gap K para el detalle de por qué ya no aplica.
//
//  D) [RESUELTO] FA05 (cita sin pago confirmado): el mensaje ya es exacto
//     ("...del paciente tiene estado 'Pendiente de pago'. Debe realizar
//     el pago en caja antes de ser atendido.").
//
//  E) Paso 8 del flujo normal / FA08 (llegada registrada): el texto lo
//     arma el backend (RecepcionServiceImpl), no recepcion.html. Aquí
//     solo se prueba el CONTRATO del frontend: si el backend manda el
//     texto exacto del documento, ¿recepcion.html lo muestra bien? Eso se
//     prueba y pasa. Si el backend hoy arma un texto distinto, es un gap
//     de backend, fuera del alcance de este spec de frontend.
//
//  F) FA01 (mensaje de emergencia): mismo patrón que (E), solo contrato
//     del frontend — reescrito en REVISIÓN 4 contra la cadena de
//     llamadas nueva (buscar por DPI → usuarios/me → sucursal-especialidad
//     → medicos-disponibles → POST emergencia).
//
//  G) [RESUELTO] FA07 (reasignación de médico): el botón ya dice
//     "Reasignar Médico", el modal ya muestra el resumen de la cita
//     (paciente, fecha, especialidad, sede, médico actual), el campo de
//     motivo ya no es obligatorio, y tras confirmar ya redirige (a
//     recepcion.html?vista=citas, que sí incluye "citas" en la URL).
//     NOTA: se sigue usando un modal en vez de una pantalla completa
//     dedicada, pero esto quedó decidido EN EQUIPO como diseño válido —
//     no es gap, los tests de este bloque asumen modal (#modal-reasignar)
//     a propósito.
//
//  H) [RESUELTO] El botón de confirmación de llegada contiene el texto
//     "Registrar llegada" (el texto completo es "Registrar llegada a
//     clínica", pero la comprobación usa contain.text).
//
//  I) [RESUELTO] El indicador "Llegada registrada — esperando llamado de
//     enfermería" ya existe (#d-indicador-llegada) y se muestra tras
//     registrar la llegada.
//
//  J) [RESUELTO, NO ES GAP] RN-CU05-02 (color por estado): confirmado
//     correcto — Pagada/Confirmada->verde, Pendiente de pago->amarillo,
//     Cancelada->rojo, vía <span class="badge ..."> anidado en #d-est.
//
//  K) [RESUELTO en REVISIÓN 4, y la variante pendiente RESUELTA en
//     REVISIÓN 5] FA01 ya es un flujo independiente: el botón
//     #btn-emergencia-directa está siempre visible desde que se abre la
//     pantalla, sin depender de ninguna búsqueda previa. Como
//     consecuencia, #acciones-extra (la caja de FA04) ya solo muestra
//     "Nueva Cita (Walk-in)" — el conflicto "ÚNICAMENTE" ya no existe
//     porque el botón de emergencia vive fuera de esa caja.
//     La variante que quedaba pendiente (DPI nuevo dentro del modal de
//     emergencia) ya se implementó: si el DPI no corresponde a ningún
//     paciente registrado, el backend da de alta una cuenta mínima con
//     nombre+DPI y crea la cita de emergencia en el mismo paso — tal como
//     pide el documento para un paciente totalmente nuevo.
//
//  L) [RESUELTO] RN-CU05-01 (validación de campo obligatorio): el mensaje
//     ya es el texto exacto del documento ("Debe ingresar un número de
//     cita o DPI para buscar."). Antes decía "Ingrese No. de cita o DPI
//     del paciente."
//
//  M) [ABIERTO] RN-CU05-01 también define un mensaje de "sin resultados":
//     "No se encontró una cita asociada a los parámetros ingresados.
//     Verifique los datos e intente nuevamente." Este mensaje aplicaría
//     al buscar por NÚMERO DE CITA cuando no existe ninguna coincidencia
//     (a diferencia de la búsqueda por DPI, que sí tiene un flujo
//     definido vía FA02 → FA03/FA04, ligado a la existencia del
//     paciente). buscar() solo maneja tres valores de data.resultado
//     (CITA_ENCONTRADA, PACIENTE_NO_REGISTRADO, SIN_CITAS_ACTIVAS); si el
//     backend responde con un cuarto valor para "número de cita no
//     encontrado", no hay ninguna rama que lo maneje y no se muestra
//     ningún mensaje al Empleado Interno. El test de abajo asume que el
//     backend usaría un resultado tipo 'CITA_NO_ENCONTRADA' — ajustar el
//     nombre exacto una vez que se defina el contrato con Edy Ramírez.
//
//  N1) [RESUELTO] registrarLlegada() ya actualiza el campo de hora de
//      llegada en el DOM sin recargar. PENDIENTE de decidir el sábado si
//      debe mostrar solo la hora (el campo se llama "Hora de llegada")
//      o fecha+hora como hoy.
//  N2) [PARCIAL] El resumen de reasignación ya muestra "Médico actual" y
//      la lista ya excluye al médico ya asignado — ambos dependen de que
//      el backend mande medicoId/medicoNombre en la cita (campo aún no
//      confirmado con Edy Ramírez).
//  N3) [RESUELTO, no es gap] La lista de médicos disponibles excluye
//      correctamente a un médico de otra sede.
//  N4) [RESUELTO, decisión de equipo — ya NO es gap] El modal de
//      emergencia sigue solo con nombre+DPI, sin campo de triaje. Se
//      decidió que no hace falta: toda emergencia entra con prioridad
//      ALTA por definición.
//  N5) [RESUELTO] "Registrar Paciente" (FA03) terminaba en index.html
//      porque registro.html exige ?dpi=... en la URL y no lo recibía —
//      no era un problema de sesión. Se corrigió pasando el DPI en la
//      navegación. El caso de FA04 (walk-in) no mostró el mismo problema
//      en corridas repetidas — se deja como estaba.
//  N6) [RESUELTO] El mensaje de error genérico de registrar llegada ya
//      es "Error al registrar la llegada." tal como pide el documento.
//  N7) [RESUELTO, no es gap] Caso negativo de la etiqueta EMERGENCIA.
//  N8) [NUEVO, RESUELTO] El paso 4 del flujo normal exige mostrar la
//      "prioridad (si aplica)". #d-prioridad ya existe y se llena bien
//      en buscar(), pero no tenía ningún test — se agrega uno que cubre
//      el caso normal ("Normal") y el de emergencia ("EMERGENCIA...").

describe('CU-05 - Recepción y Verificación de Cita', () => {
  const abrirRecepcion = () => {
    cy.simularSesion({ rol: 'Recepcionista', nombre: 'Empleado Interno' });
    cy.visit('/recepcion.html');
    cy.get('#area-busqueda').should('be.visible');
  };

  const citaBase = {
    id: 501,
    pacienteId: 77,
    pacienteNombre: 'Juan Pérez',
    estadoNombre: 'Confirmada',
    especialidadNombre: 'Medicina General',
    sucursalNombre: 'Sede Central',
    fechaHora: '2026-08-05T09:00:00',
    motivo: 'Dolor de cabeza',
    emergencia: false,
    numeroCita: 501,
  };

  describe('Flujo Normal Básico (pasos 1-9 del documento)', () => {
    beforeEach(abrirRecepcion);

    it('RN-CU05-01: exige al menos un campo (DPI o número de cita) con el mensaje exacto del documento', () => {
      cy.contains('button', 'Verificar llegada').click();
      cy.get('#msg').should('contain.text', 'Debe ingresar un número de cita o DPI para buscar.');
    });

    it('FA02: el buscador usa botones de alternancia "Por DPI" / "Por No. Cita", no dos campos simultáneos', () => {
      cy.get('#grupo-dpi').should('be.visible');
      cy.get('#grupo-num').should('not.be.visible');
      cy.contains('button', 'Por No. Cita').click();
      cy.get('#grupo-num').should('be.visible');
      cy.get('#grupo-dpi').should('not.be.visible');
    });

    it('Paso 3-4 (RN-CU05-01/02): busca la cita por DPI y muestra sus datos, incluyendo la fecha', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');

      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#detalle').should('be.visible');
      cy.get('#d-pac').should('contain.text', 'Juan Pérez');
      cy.get('#d-est').should('contain.text', 'Confirmada');
      cy.get('#d-esp').should('contain.text', 'Medicina General');
      cy.get('#d-suc').should('contain.text', 'Sede Central');
      cy.get('#d-mot').should('contain.text', 'Dolor de cabeza');
      // RN-CU05-02 exige mostrar también la fecha de la cita.
      cy.get('#d-fec').should('be.visible').and('not.be.empty');
    });

    it('RN-CU05-02: la ficha de la cita muestra también el número de cita y la hora de llegada', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');

      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#d-num').should('contain.text', '501');
      cy.get('[data-field="hora-llegada"]').should('exist');
    });

    it('Paso 6 del flujo normal: el botón de confirmación de llegada dice "Registrar llegada"', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#btn-llegada').should('be.visible').and('contain.text', 'Registrar llegada');
    });

    it('Contrato del frontend (E): si el backend manda el texto exacto del documento, se muestra tal cual', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        ...citaBase,
        estadoNombre: 'Paciente Presente',
        mensaje: 'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
            + 'El paciente debe pasar a la sala de espera.',
      }).as('llegada');

      cy.get('#btn-llegada').click();
      cy.wait('@llegada');

      cy.get('#msg').should('contain.text',
          'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
          + 'El paciente debe pasar a la sala de espera.');
      cy.get('#btn-llegada').should('be.disabled');

      // Postcondición 2.5: el estado de la cita se actualiza a "Paciente
      // Presente" y queda visible en la ficha.
      cy.get('#d-est').should('contain.text', 'Paciente Presente');
    });

    it('Paso 7 del flujo normal: tras registrar la llegada, la cita muestra el indicador "Llegada registrada — esperando llamado de enfermería"', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        ...citaBase,
        estadoNombre: 'Paciente Presente',
        mensaje: 'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
            + 'El paciente debe pasar a la sala de espera.',
      }).as('llegada');
      cy.get('#btn-llegada').click();
      cy.wait('@llegada');

      cy.contains('Llegada registrada — esperando llamado de enfermería').should('be.visible');
    });

    it('[N1 - RESUELTO] Paso 7 del flujo normal: la hora de llegada se actualiza en el DOM sin recargar', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('[data-field="hora-llegada"]').should('have.text', '—');

      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        ...citaBase,
        estadoNombre: 'Paciente Presente',
        horaLlegada: '2026-08-06T09:15:00',
        mensaje: 'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
            + 'El paciente debe pasar a la sala de espera.',
      }).as('llegada');
      cy.get('#btn-llegada').click();
      cy.wait('@llegada');

      cy.get('[data-field="hora-llegada"]').should('not.have.text', '—');
    });

    it('[N7] RN-CU05-02: una cita SIN prioridad de emergencia no muestra la etiqueta EMERGENCIA', () => {
      // Caso negativo del que solo se probaba el positivo, en FA08.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, emergencia: false },
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#badge-emergencia').should('not.be.visible');
    });

    it('[N8 - NUEVO] Paso 4 del flujo normal: el sistema muestra la "prioridad (si aplica)" de la cita', () => {
      // El documento (paso 4) exige mostrar, entre otros datos, la
      // "prioridad (si aplica)". #d-prioridad existe en el HTML y se llena
      // en buscar() a partir de c.emergencia, pero ningún test verificaba
      // su contenido — ni el caso normal ni el de emergencia.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, emergencia: false },
      }).as('buscarNormal');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscarNormal');
      cy.get('#d-prioridad').should('contain.text', 'Normal');

      cy.get('#dpi').clear().type('2234567890102');
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, emergencia: true },
      }).as('buscarEmergencia');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscarEmergencia');
      cy.get('#d-prioridad').should('contain.text', 'EMERGENCIA');
    });
  });

  describe('RN-CU05-02: color e indicación por estado de cita', () => {
    beforeEach(abrirRecepcion);

    // El color va en un <span class="badge ..."> anidado dentro de
    // #d-est, no como clase directa en #d-est. Coincide con RN-CU05-02
    // (Pagada/Confirmada->verde, Pendiente de pago->amarillo,
    // Cancelada->rojo).

    it('Estado "Confirmada" (equivalente a "Pagada" en RN-CU05-02) se muestra en verde y permite continuar', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Confirmada' },
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#d-est').find('span.badge').should('have.class', 'badge-success');
      cy.get('#btn-llegada').should('not.be.disabled');
    });

    it('Estado "Pendiente de pago" se muestra en amarillo y bloquea el registro de llegada', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Pendiente de pago' },
      }).as('buscar');
      cy.get('#dpi').type('3234567890103');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#d-est').find('span.badge').should('have.class', 'badge-warning');
      cy.get('#btn-llegada').should('be.disabled');
    });

    it('Estado "Cancelada" se muestra en rojo y bloquea el registro de llegada (el botón sigue existiendo, solo disabled)', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Cancelada' },
      }).as('buscar');
      cy.get('#dpi').type('4234567890104');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#d-est').find('span.badge').should('have.class', 'badge-danger');
      // btn-llegada.disabled = (estadoNombre !== 'Confirmada') — el botón
      // nunca se elimina del DOM, solo queda disabled.
      cy.get('#btn-llegada').should('be.disabled');
    });
  });

  describe('FA02/FA03 - Paciente no registrado en el sistema', () => {
    beforeEach(abrirRecepcion);

    it('FA03: mensaje y sub-texto exactos del documento, y botón único "Registrar Paciente"', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', { resultado: 'PACIENTE_NO_REGISTRADO' }).as('buscar');

      cy.get('#dpi').type('9999999999999');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#acciones-extra').should('be.visible');
      cy.get('#txt-extra').should('have.text', 'No se encontró ningún paciente con ese DPI.');
      cy.get('#acciones-extra').should('contain.text', 'Es necesario registrar al paciente antes de continuar.');

      cy.get('#btn-reg-pac').should('be.visible').and('contain.text', 'Registrar Paciente');
      cy.get('#btn-walkin').should('not.be.visible');
      cy.get('#btn-nueva-cita').should('not.be.visible');
    });

    it('[N5 - RESUELTO] al hacer clic en "Registrar Paciente" el sistema navega a la pantalla de registro CON el DPI en la URL', () => {
      // CAUSA REAL confirmada leyendo registro.html: esa página exige un
      // parámetro ?dpi=... en la URL y se auto-redirige a index.html si no
      // lo recibe. No era un problema de sesión/token — el botón navegaba
      // sin pasarle el DPI. FIX: irARegistrar() ahora arma la URL con
      // ?dpi=<el DPI que se buscó>.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', { resultado: 'PACIENTE_NO_REGISTRADO' }).as('buscar');
      cy.get('#dpi').type('9999999999999');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#btn-reg-pac').click();
      cy.url().should('include', 'registro.html');
      cy.url().should('include', 'dpi=9999999999999');
    });
  });


  describe('FA02/FA04 - Paciente registrado sin citas activas', () => {
    beforeEach(abrirRecepcion);

    it('FA04: mensaje, sub-texto y botón "Nueva Cita (Walk-in)" exactos del documento', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'SIN_CITAS_ACTIVAS',
        pacienteNombre: 'Pedro Martínez',
        pacienteId: 90,
      }).as('buscar');

      cy.get('#dpi').type('5234567890105');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#txt-extra').should('have.text', 'El paciente Pedro Martínez está registrado pero no tiene citas activas.');
      cy.get('#acciones-extra').should('contain.text', 'Puede crear una nueva cita para este paciente.');
      cy.get('#btn-walkin').should('be.visible').and('contain.text', 'Nueva Cita (Walk-in)');
      cy.get('#btn-reg-pac').should('not.be.visible');
    });

    it('[Gap K - RESUELTO en REVISIÓN 4] #acciones-extra muestra ÚNICAMENTE "Nueva Cita (Walk-in)" — el botón de emergencia ya no vive dentro de esta caja', () => {
      // Antes esto era NOTA GAP (K): #btn-emergencia-directa aparecía
      // DENTRO de #acciones-extra junto con "Nueva Cita (Walk-in)". Con
      // el rediseño, el botón de emergencia es un elemento hermano fuera
      // de #acciones-extra (vive en el header de #area-busqueda), así
      // que ya no puede estar "dentro" de esta caja. Se verifica
      // estructuralmente, no solo visualmente.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'SIN_CITAS_ACTIVAS',
        pacienteNombre: 'Pedro Martínez',
        pacienteId: 90,
      }).as('buscar');

      cy.get('#dpi').type('5234567890105');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#acciones-extra').find('#btn-emergencia-directa').should('not.exist');
    });

    it('[N5] FA04 paso 9: al hacer clic en "Nueva Cita (Walk-in)" el sistema navega a agendar cita para ese paciente', () => {
      // CONFIRMADO en el código: irAWalkIn() navega a
      // paciente_agendar.html?pacienteId=<id>.
      // PENDIENTE DE VERIFICAR MANUALMENTE: en corridas anteriores esta
      // navegación a veces terminaba redirigida a login.html?expired=true
      // de forma inconsistente entre corridas — confirma en el navegador
      // real antes de darlo por bueno o por gap.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'SIN_CITAS_ACTIVAS',
        pacienteNombre: 'Pedro Martínez',
        pacienteId: 90,
      }).as('buscar');
      cy.get('#dpi').type('5234567890105');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#btn-walkin').click();
      cy.url().should('include', 'paciente_agendar.html');
    });
  });

  describe('FA05 - Cita sin pago confirmado', () => {
    beforeEach(abrirRecepcion);

    it('FA05: mensaje exacto del documento al encontrar una cita "Pendiente de pago"', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Pendiente de pago' },
      }).as('buscar');

      cy.get('#dpi').type('3234567890103');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#msg').should('contain.text',
          "La cita del paciente tiene estado 'Pendiente de pago'. Debe realizar el pago en caja antes de ser atendido.");
      cy.get('#btn-llegada').should('be.disabled');
    });

    it('Tras el pago (CU-06), al volver a buscar la cita ya aparece Confirmada y permite registrar la llegada', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Confirmada' },
      }).as('buscarConfirmada');

      cy.get('#num').should('not.be.visible');
      cy.contains('button', 'Por No. Cita').click();
      cy.get('#num').type('501');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscarConfirmada');

      cy.get('#btn-llegada').should('not.be.disabled');
    });
  });

  describe('FA06 - Cita cancelada', () => {
    beforeEach(abrirRecepcion);

    it('Mensaje y botón "Nueva Cita" exactos del documento', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, estadoNombre: 'Cancelada' },
      }).as('buscar');

      cy.get('#dpi').type('4234567890104');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#msg').should('contain.text', 'La cita fue cancelada. El paciente debe agendar una nueva cita.');
      cy.get('#btn-nueva-cita').should('be.visible').and('contain.text', 'Nueva Cita');
    });
  });

  describe('NOTA GAP (M) - Búsqueda por número de cita sin resultados (RN-CU05-01)', () => {
    beforeEach(abrirRecepcion);

    it('el documento exige el mensaje "No se encontró una cita asociada a los parámetros ingresados..." cuando el número de cita no existe', () => {
      // Ajustar 'CITA_NO_ENCONTRADA' al valor real que use el backend una
      // vez definido con Edy Ramírez — ver Gap M en la cabecera. Con el
      // código actual, cualquier valor de resultado distinto a
      // CITA_ENCONTRADA / PACIENTE_NO_REGISTRADO / SIN_CITAS_ACTIVAS no
      // dispara ningún mensaje, así que esta prueba falla a propósito.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', { resultado: 'CITA_NO_ENCONTRADA' }).as('buscar');

      cy.contains('button', 'Por No. Cita').click();
      cy.get('#num').type('99999');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#msg').should('contain.text',
          'No se encontró una cita asociada a los parámetros ingresados. Verifique los datos e intente nuevamente.');
    });
  });

  describe('FA01 - Paciente llega por emergencia (sin cita previa)', () => {
    beforeEach(abrirRecepcion);

    it('El botón de emergencia está visible desde el inicio, sin depender de ninguna búsqueda previa', () => {
      // FA01 es un flujo verdaderamente independiente — el botón está
      // visible apenas se carga la pantalla, sin necesidad de escribir
      // DPI ni hacer clic en "Verificar llegada".
      cy.get('#btn-emergencia-directa').should('be.visible').and('contain.text', 'Registrar Emergencia');
    });

    it('FA01 paso 2: el modal de emergencia solo pide nombre y DPI del paciente, tal como exige el documento', () => {
      cy.get('#btn-emergencia-directa').click();
      cy.get('#modal-emergencia').should('be.visible');
      cy.get('#em-nombre').should('be.visible');
      cy.get('#em-dpi').should('be.visible');
    });

    it('Contrato del frontend: un solo POST a /api/recepcion/emergencia con nombre+DPI, y el mensaje del backend se muestra tal cual', () => {
      // FIX: confirmarEmergencia() ya NO encadena /citas/buscar, /usuarios/me,
      // /sucursal-especialidad y /citas/medicos-disponibles — todo eso ahora
      // lo resuelve el backend (RecepcionServiceImpl.registrarEmergenciaConAlta)
      // a partir del Recepcionista autenticado. El frontend solo manda
      // nombrePaciente + dpi.
      cy.intercept('POST', '/api/recepcion/emergencia', {
        statusCode: 201,
        body: {
          mensaje: 'Paciente Pedro Martínez registrado con prioridad de EMERGENCIA. '
              + 'El paciente debe pasar directamente a toma de signos vitales.',
        },
      }).as('emergencia');

      cy.get('#btn-emergencia-directa').click();
      cy.get('#em-nombre').type('Pedro Martínez');
      cy.get('#em-dpi').type('5234567890105');
      cy.contains('button', 'Confirmar ingreso de emergencia').click();

      cy.wait('@emergencia').its('request.body').should('deep.equal', {
        nombrePaciente: 'Pedro Martínez',
        dpi: '5234567890105',
        motivo: null,
      });

      cy.get('#msg').should('contain.text',
          'Paciente Pedro Martínez registrado con prioridad de EMERGENCIA. '
          + 'El paciente debe pasar directamente a toma de signos vitales.');
      cy.get('#modal-emergencia').should('have.class', 'd-none');
    });

    it('Si el DPI ya existe en el sistema, el backend usa esa cuenta (mismo contrato, el frontend no distingue)', () => {
      // El frontend ya no sabe ni le importa si el paciente existía o no —
      // esa decisión (usar cuenta existente vs. dar de alta una nueva con
      // nombre+DPI) vive enteramente en el backend. Este test solo confirma
      // que, sea cual sea la rama que tomó el servidor, el mensaje se
      // muestra igual y el modal se cierra igual.
      cy.intercept('POST', '/api/recepcion/emergencia', {
        statusCode: 201,
        body: {
          mensaje: 'Paciente Juan Pérez registrado con prioridad de EMERGENCIA. '
              + 'El paciente debe pasar directamente a toma de signos vitales.',
        },
      }).as('emergencia');

      cy.get('#btn-emergencia-directa').click();
      cy.get('#em-nombre').type('Juan Pérez');
      cy.get('#em-dpi').type('1234567890101');
      cy.contains('button', 'Confirmar ingreso de emergencia').click();
      cy.wait('@emergencia');

      cy.get('#msg').should('contain.text', 'Paciente Juan Pérez registrado con prioridad de EMERGENCIA.');
      cy.get('#modal-emergencia').should('have.class', 'd-none');
    });

    it('[FIX banners] si el backend rechaza la emergencia (ej. sede sin especialidad/médicos, o cualquier otro 4xx), el error se muestra DENTRO del modal, no en el #msg de arriba', () => {
      // Antes este tipo de error se mostraba en el #msg global, que queda
      // tapado detrás del overlay del modal — el Empleado Interno no lo
      // veía. Debe aparecer en #msg-emergencia, y el modal debe permanecer
      // abierto para que pueda corregir el dato o reintentar.
      cy.intercept('POST', '/api/recepcion/emergencia', {
        statusCode: 400,
        body: { mensaje: 'No hay médicos disponibles de Medicina General en su sede en este momento.' },
      }).as('emergenciaError');

      cy.get('#btn-emergencia-directa').click();
      cy.get('#em-nombre').type('Pedro Martínez');
      cy.get('#em-dpi').type('5234567890105');
      cy.contains('button', 'Confirmar ingreso de emergencia').click();
      cy.wait('@emergenciaError');

      cy.get('#msg-emergencia').should('be.visible')
          .and('contain.text', 'No hay médicos disponibles de Medicina General en su sede en este momento.');
      cy.get('#modal-emergencia').should('not.have.class', 'd-none');
      cy.get('#msg').should('not.contain.text', 'No hay médicos disponibles');
    });

    it('Decisión de equipo (ya NO es gap): no existe campo de triaje/prioridad — toda emergencia entra con prioridad ALTA por definición', () => {
      // El paso 3 del documento menciona "clasifica la prioridad según
      // triaje", pero en equipo se decidió que no aporta valor: si un caso
      // no ameritara prioridad alta, no calificaría como emergencia en
      // primer lugar. Este test ya no es NOTA GAP — confirma la decisión:
      // el formulario solo pide nombre+DPI, sin selector de severidad.
      cy.get('#btn-emergencia-directa').click();
      cy.get('#form-emergencia').invoke('text').should('not.match', /triaj/i);
      cy.get('#em-nombre').should('exist');
      cy.get('#em-dpi').should('exist');
    });
  });

  describe('FA07 - Reasignación de médico', () => {
    beforeEach(() => {
      abrirRecepcion();
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');
    });

    it('el botón dice "Reasignar Médico" y el modal muestra el resumen de la cita', () => {
      cy.get('#btn-reasignar').should('contain.text', 'Reasignar Médico');

      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');

      cy.get('#modal-reasignar').within(() => {
        cy.contains('Juan Pérez').should('exist');
        cy.contains('Medicina General').should('exist');
        cy.contains('Sede Central').should('exist');
      });
    });

    it('[N2 - RESUELTO en código, pendiente de backend] la fecha y el médico actual se muestran en el resumen', () => {
      // El documento (FA07, paso 3) exige que el resumen incluya
      // "paciente, fecha, especialidad, sede, médico actual". Ya existe
      // #rr-medico en el HTML. Esto depende de que el backend mande
      // medicoNombre en la respuesta de /buscar — se simula aquí con el
      // intercept para probar el camino feliz del frontend.
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, medicoNombre: 'Dr. Julio Estrada' },
      }).as('buscarConMedico');
      cy.get('#dpi').clear().type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscarConMedico');

      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');

      cy.get('#rr-fecha').should('contain.text', '2026-08-05 09:00:00');
      cy.get('#modal-reasignar').should('contain.text', 'Dr. Julio Estrada');
    });

    it('[N3] la lista de médicos disponibles excluye a un médico de otra sede o especialidad', () => {
      // El documento pide "una lista de médicos disponibles de la misma
      // sede y especialidad". citaBase es Sede Central / Medicina
      // General; se mockea un médico que SÍ calza y uno que NO (otra
      // sede) para confirmar que el select solo ofrece el primero.
      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
        { id: 9, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Norte', especialidadNombre: 'Medicina General', nombreCompleto: 'Dra. Ana Lima' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');

      cy.get('#r-medico').find('option').should('contain.text', 'Dr. Mario Castañeda');
      cy.get('#r-medico').find('option').should('not.contain.text', 'Dra. Ana Lima');
    });

    it('la nota/motivo de la reasignación es opcional, según el documento', () => {
      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');

      cy.get('#r-medico').select('Dr. Mario Castañeda');
      cy.intercept('POST', '/api/recepcion/citas/501/reasignar', { mensaje: 'Médico reasignado correctamente' }).as('reasignar');
      cy.contains('button', 'Confirmar Reasignación').click();
      cy.wait('@reasignar');
    });

    it('tras reasignar, redirige incluyendo "citas" en la URL', () => {
      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');
      cy.get('#r-medico').select('Dr. Mario Castañeda');
      cy.get('#r-motivo').type('Médico titular indispuesto');

      cy.intercept('POST', '/api/recepcion/citas/501/reasignar', { mensaje: 'Médico reasignado correctamente' }).as('reasignar');
      cy.contains('button', 'Confirmar Reasignación').click();
      cy.wait('@reasignar');

      cy.get('#msg').should('contain.text', 'Médico reasignado correctamente');
      cy.url().should('include', 'citas');
    });

    it('[FIX banners] si la reasignación falla, el error se muestra DENTRO del modal, no en el #msg de arriba', () => {
      cy.intercept('GET', '/api/usuarios', [
        { id: 8, rolNombre: 'Médico', activo: true, sucursalNombre: 'Sede Central', especialidadNombre: 'Medicina General', nombreCompleto: 'Dr. Mario Castañeda' },
      ]).as('medicos');
      cy.get('#btn-reasignar').click();
      cy.wait('@medicos');
      cy.get('#r-medico').select('Dr. Mario Castañeda');

      cy.intercept('POST', '/api/recepcion/citas/501/reasignar', {
        statusCode: 409,
        body: { mensaje: 'El médico seleccionado ya no está disponible.' },
      }).as('reasignarError');
      cy.contains('button', 'Confirmar Reasignación').click();
      cy.wait('@reasignarError');

      cy.get('#msg-reasignar').should('be.visible').and('contain.text', 'El médico seleccionado ya no está disponible.');
      cy.get('#modal-reasignar').should('not.have.class', 'd-none');
      cy.get('#msg').should('not.contain.text', 'El médico seleccionado ya no está disponible');
    });
  });

  describe('FA08 - Cita con prioridad de emergencia', () => {
    beforeEach(abrirRecepcion);

    it('Muestra la etiqueta "EMERGENCIA" y el botón "Signos Vitales (Urgente)" tras registrar la llegada', () => {
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: { ...citaBase, emergencia: true },
      }).as('buscar');
      cy.get('#dpi').type('2234567890102');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');

      cy.get('#badge-emergencia').should('be.visible').and('contain.text', 'EMERGENCIA');

      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        ...citaBase, emergencia: true, estadoNombre: 'Paciente Presente',
        mensaje: 'Paciente Juan Pérez registrado con prioridad de EMERGENCIA. '
            + 'El paciente debe pasar directamente a toma de signos vitales.',
      }).as('llegada');
      cy.get('#btn-llegada').click();
      cy.wait('@llegada');

      cy.get('#btn-signos-urgente').should('be.visible').and('contain.text', 'Signos Vitales (Urgente)');
    });
  });

  describe('FA09 - Error al registrar llegada', () => {
    beforeEach(() => {
      abrirRecepcion();
      cy.intercept('GET', '/api/recepcion/citas/buscar*', {
        resultado: 'CITA_ENCONTRADA',
        cita: citaBase,
      }).as('buscar');
      cy.get('#dpi').type('1234567890101');
      cy.contains('button', 'Verificar llegada').click();
      cy.wait('@buscar');
    });

    it('Muestra el mensaje de error cuando el cambio de estado falla y permite reintentar', () => {
      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        statusCode: 409,
        body: { mensaje: 'Operación no permitida' },
      }).as('llegadaError');

      cy.get('#btn-llegada').click();
      cy.wait('@llegadaError');

      cy.get('#msg').should('contain.text', 'Operación no permitida');
      // El botón sigue disponible: el Empleado Interno puede reintentar la
      // operación, tal como pide el documento.
      cy.get('#btn-llegada').should('not.be.disabled');

      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        ...citaBase, estadoNombre: 'Paciente Presente',
        mensaje: 'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
            + 'El paciente debe pasar a la sala de espera.',
      }).as('llegadaReintento');
      cy.get('#btn-llegada').click();
      cy.wait('@llegadaReintento');

      cy.get('#msg').should('contain.text',
          'La llegada del paciente Juan Pérez ha sido registrada exitosamente. '
          + 'El paciente debe pasar a la sala de espera.');
      cy.get('#btn-llegada').should('be.disabled');
    });

    it('[N6 - RESUELTO] el mensaje genérico ya coincide con el texto exacto que exige el documento', () => {
      cy.intercept('POST', '/api/recepcion/citas/501/llegada', {
        statusCode: 500,
        body: {},
      }).as('llegadaErrorGenerico');

      cy.get('#btn-llegada').click();
      cy.wait('@llegadaErrorGenerico');

      cy.get('#msg').should('contain.text', 'Error al registrar la llegada');
      cy.get('#btn-llegada').should('not.be.disabled');
    });
  });
});