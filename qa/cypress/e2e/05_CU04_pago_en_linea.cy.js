// Ubicación en tu proyecto: qa/cypress/e2e/05_CU04_pago_en_linea.cy.js
//
// IMPORTANTE (léelo antes de correr esto): a diferencia de la cobertura
// "de paso" que ya tenía CU-04 dentro de 04_CU03_agendar_citas.cy.js (que
// valida contra el texto REAL de la app para no generar falsos negativos),
// este spec está escrito CONTRA EL DOCUMENTO 4_CU_Pago_en_Linea.docx. Donde
// el frontend real (paciente_citas.html) se desvía del texto/las reglas del
// documento, la prueba queda tal cual el documento lo exige y por lo tanto
// VA A FALLAR — con un comentario "NOTA GAP" explicando la diferencia
// encontrada. No se ajustó ningún selector ni mock para maquillar el
// resultado.
//
// Gaps de FRONTEND encontrados entre el documento y paciente_citas.html:
//
//  A) Doc (paso 9): el botón debe cambiar a "Procesando pago...". El
//     código real (js embebido) pone btn.innerText = "Autorizando con el
//     banco...".
//  B) Doc (FA01): mensaje de tarjeta inválida = "El número de tarjeta no
//     es válido". El código real muestra "El número de tarjeta ingresado
//     no es válido."
//  C) Doc (FA01): mensaje de tarjeta vencida = "La tarjeta está vencida".
//     El código real muestra "La tarjeta de crédito/débito se encuentra
//     vencida."
//  D) Doc (FA02): mensaje completo de reserva expirada (ver test). El
//     código real omite "su cita"/"seleccionado" y la frase "Por favor,
//     seleccione un nuevo horario." — sí conserva "Será redirigido en
//     unos segundos...".
//  E) Doc (paso 13): mensaje de éxito "¡Pago realizado exitosamente!
//     Número de transacción: [Número]. Su cita ha sido confirmada." El
//     frontend real NO muestra ningún mensaje con ese texto: pasa
//     directo del formulario al comprobante (#area-recibo).
//
// NOTA sobre el estado de la cita (paso 12 del documento / postcondición
// 2.5): el documento pide que el estado quede como "Pagada", pero se
// decidió a propósito dejarlo como "Confirmada" para no agregar una
// columna/estado extra. No es un gap de implementación pendiente de
// corregir — es una desviación intencional del documento, por eso no se
// prueba el estado "Pagada" en ningún test de este archivo.
//
// NOTA sobre el CVV (paso 6 del documento / pendiente con Ing. Edy
// Ramírez, se revisa el sábado): el documento dice "El CVV nunca se
// almacena ni se envía", pero el payload real de POST /api/pagos SÍ
// incluye el campo cvv en el cuerpo de la petición. Por ahora este spec
// documenta el comportamiento REAL (que el CVV se envía) en vez de
// marcarlo como gap, hasta que se defina el tema con el ingeniero.
//
// Los 3 mensajes exactos de FA03 (rechazo bancario / error de
// procesamiento / error de comunicación) NO se prueban aquí contra el
// backend real: eso se cubre en PagoServiceImplTest.java (Mockito), que sí
// ejecuta el código real del servicio y encuentra que ninguno de los dos
// caminos implementados coincide con los 3 textos del documento (ver NOTA
// GAP #4 allá). Aquí, con cy.intercept, solo se puede probar el CONTRATO
// del frontend: si el backend llega a mandar el texto correcto, ¿el
// frontend lo muestra bien y deja reintentar sin perder la reserva? Eso sí
// se prueba y sí pasa; se deja explícito en cada test para no confundirlo
// con una prueba contra el backend real.
//
// NOTA sobre los bloques agregados al final (resumen completo del paso 1,
// límites de RN-CU04-01/02/04, y el 3er mensaje de FA01): no se tuvo
// acceso a paciente_citas.html al escribirlos, así que los selectores
// #p-medico, #p-especialidad, #p-sede, #p-fecha y #p-hora son una
// SUPOSICIÓN siguiendo la convención #p-<campo> del resto del archivo —
// verifícalos contra el HTML real antes de confiar en un fallo o un pase.
// Los números de tarjeta de 12/13/19/20 dígitos sí están verificados con
// el algoritmo de Luhn (todos pasan Luhn, así que cualquier rechazo que
// veas en los de 13 y 19 dígitos es por longitud, no por el checksum).

describe('CU-04 - Pago en Línea de la Cita Médica', () => {
  const TARJETA_VALIDA = '4111111111111111';

  const reciboExitoso = {
    numeroTransaccion: 'TRX-000123',
    fechaHoraCita: '2026-08-10T09:00:00',
    medicoNombre: 'Dr. Marco Solís',
    especialidadNombre: 'Medicina General',
    sucursalNombre: 'Sede Central',
    monto: 150,
  };

  const abrirPantallaDePago = () => {
    cy.simularSesion({ rol: 'Paciente', nombre: 'Ana López' });
    cy.intercept('GET', '/api/citas/mis-citas', []).as('misCitas');
    cy.intercept('GET', '/api/caja/citas/buscar*', []).as('buscarCitas');
    cy.visit('/paciente_citas.html?autoPay=900&monto=150');
    cy.wait(['@misCitas', '@buscarCitas']);
    cy.get('#area-pago').should('be.visible');
  };

  const llenarFormularioValido = () => {
    cy.get('#p-tarjeta').type(TARJETA_VALIDA);
    cy.get('#p-titular').type('ana lopez');
    cy.get('#p-venc').type('1230');
    cy.get('#p-cvv').type('123');
  };

  describe('Flujo normal básico (pasos 1-15 del documento)', () => {
    beforeEach(abrirPantallaDePago);

    it('Paso 1: muestra el resumen de la cita, el temporizador de 5 minutos y el total en Quetzales', () => {
      cy.get('#p-citaIdText').should('contain.text', '900');
      cy.get('#p-monto').should('contain.text', '150');
      cy.get('#timer').should('contain.text', '05:00');
    });

    // AGREGADO — el documento (paso 1) exige también médico, especialidad,
    // sede, fecha y hora en el resumen, no solo el monto y el timer.
    it('Paso 1 (completo): el resumen también muestra médico, especialidad, sede, fecha y hora', () => {
      // TODO: confirmar estos selectores contra paciente_citas.html real.
      cy.get('#p-medico').should('not.be.empty');
      cy.get('#p-especialidad').should('not.be.empty');
      cy.get('#p-sede').should('not.be.empty');
      cy.get('#p-fecha').should('not.be.empty');
      cy.get('#p-hora').should('not.be.empty');
    });

    it('RN-CU04-01: al perder el foco, el número de tarjeta se enmascara mostrando solo los últimos 4 dígitos', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA).blur();
      cy.get('#p-tarjeta').should('have.value', '************1111');

      // Al volver a enfocar, el documento no dice que deba restaurarse el
      // número real, pero así lo hace la app (necesario para poder
      // re-enviar el número completo al backend); se deja documentado.
      cy.get('#p-tarjeta').focus();
      cy.get('#p-tarjeta').should('have.value', TARJETA_VALIDA);
    });

    it('RN-CU04-02: el nombre del titular se convierte a mayúsculas', () => {
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-titular').should('have.value', 'ANA LOPEZ');
    });

    it('RN-CU04-03: el vencimiento se auto-formatea a MM/AA', () => {
      cy.get('#p-venc').type('1230');
      cy.get('#p-venc').should('have.value', '12/30');
    });

    it('El campo CVV es de tipo password (nota de seguridad del documento)', () => {
      cy.get('#p-cvv').should('have.attr', 'type', 'password');
    });

    it('El sistema envía la petición con la cabecera Idempotency-Key (paso 9, idempotencia mediante UUID)', () => {
      cy.intercept('POST', '/api/pagos', (req) => {
        expect(req.headers).to.have.property('idempotency-key');
        req.reply({ statusCode: 200, body: reciboExitoso });
      }).as('pagar');

      llenarFormularioValido();
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.wait('@pagar');
    });

    it('NOTA GAP (A): el botón debería decir "Procesando pago..." mientras se procesa el cobro', () => {
      cy.intercept('POST', '/api/pagos', (req) => {
        // Petición pausada a propósito para poder inspeccionar el botón
        // mientras está "procesando".
        req.reply({ statusCode: 200, body: reciboExitoso, delay: 300 });
      }).as('pagar');

      llenarFormularioValido();
      cy.contains('button', 'Procesar cargo seguro').click();

      // Texto exacto pedido por el documento (paso 9). Falla contra la app
      // real, que muestra "Autorizando con el banco...".
      cy.get('#btn-pagar').should('be.disabled').and('contain.text', 'Procesando pago...');
    });

    it('Camino feliz: procesa el pago y muestra el comprobante con los datos de RN-CU04-05', () => {
      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');

      llenarFormularioValido();
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.wait('@pagar');

      cy.get('#area-pago').should('have.class', 'd-none');
      cy.get('#area-recibo').should('be.visible');
      cy.get('#r-trx').should('contain.text', 'TRX-000123');
      cy.get('#r-med').should('contain.text', 'Dr. Marco Solís');
      cy.get('#r-esp').should('contain.text', 'Medicina General');
      cy.get('#r-suc').should('contain.text', 'Sede Central');
      cy.get('#r-mon').should('contain.text', '150');

      // Postcondición del documento: "El paciente recibe un comprobante de
      // pago en su correo electrónico" — la pantalla debe avisar de esto.
      cy.get('#area-recibo').should('contain.text', 'correo electrónico');

      // Paso 15: los dos botones de la pantalla de confirmación.
      cy.contains('button', 'Volver al Portal').should('be.visible');
      cy.contains('button', 'Ver Mis Citas').should('be.visible');
    });

    it('NOTA GAP (E): debería mostrar el mensaje de éxito con el número de transacción antes/durante el comprobante', () => {
      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');

      llenarFormularioValido();
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.wait('@pagar');

      // Texto exacto del paso 13 del documento. Falla contra la app real:
      // no existe ningún elemento con este mensaje, la app pasa directo al
      // comprobante sin mostrarlo.
      cy.get('#msg').should('contain.text', '¡Pago realizado exitosamente! Número de transacción: TRX-000123. Su cita ha sido confirmada.');
    });

    it('El CVV viaja en el cuerpo de la petición de pago (pendiente de definir con Ing. Edy Ramírez el sábado)', () => {
      cy.intercept('POST', '/api/pagos', (req) => {
        // El documento dice "El CVV nunca se almacena ni se envía.", pero
        // por ahora se documenta el comportamiento real de la app: el
        // campo cvv sí viaja en el payload. Pendiente de revisar con el
        // ingeniero el sábado si esto se corrige o se ajusta el documento.
        expect(req.body).to.have.property('cvv');
        req.reply({ statusCode: 200, body: reciboExitoso });
      }).as('pagar');

      llenarFormularioValido();
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.wait('@pagar');
    });
  });

  // AGREGADO — RN-CU04-01: el documento exige 13-19 dígitos, además de
  // Luhn. Todos los números de abajo SÍ pasan Luhn, para aislar la
  // validación de longitud (que el spec original no probaba).
  describe('RN-CU04-01: número de tarjeta debe tener entre 13 y 19 dígitos', () => {
    beforeEach(abrirPantallaDePago);

    const CASOS = [
      { numero: '411111111117', valido: false, nombre: '12 dígitos (por debajo del mínimo)' },
      { numero: '4111111111119', valido: true, nombre: '13 dígitos (mínimo permitido)' },
      { numero: '4111111111111111110', valido: true, nombre: '19 dígitos (máximo permitido)' },
      { numero: '41111111111111111115', valido: false, nombre: '20 dígitos (por encima del máximo)' },
    ];

    CASOS.forEach(({ numero, valido, nombre }) => {
      it(`${nombre}: ${valido ? 'se acepta' : 'se rechaza'}`, () => {
        cy.get('#p-tarjeta').type(numero);
        cy.get('#p-titular').type('ana lopez');
        cy.get('#p-venc').type('1230');
        cy.get('#p-cvv').type('123');

        if (valido) {
          cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');
          cy.contains('button', 'Procesar cargo seguro').click();
          cy.get('#p-tarjeta').should('not.have.class', 'is-invalid');
        } else {
          cy.contains('button', 'Procesar cargo seguro').click();
          cy.get('#p-tarjeta').should('have.class', 'is-invalid');
        }
      });
    });
  });

  // AGREGADO — RN-CU04-02: el documento exige mínimo 5 y máximo 100
  // caracteres para el nombre del titular; el spec original solo probaba
  // la conversión a mayúsculas.
  describe('RN-CU04-02: nombre del titular entre 5 y 100 caracteres', () => {
    beforeEach(abrirPantallaDePago);

    it('rechaza un titular con menos de 5 caracteres', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana'); // 3 caracteres
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-titular').should('have.class', 'is-invalid');
    });

    it('rechaza un titular con más de 100 caracteres', () => {
      const nombreLargo = 'a'.repeat(101);
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      // maxlength="100" trunca físicamente lo que se puede teclear, así que
      // para forzar un valor de 101 caracteres hay que inyectarlo directo,
      // igual que en el test de vencimiento con formato incorrecto.
      cy.get('#p-titular').invoke('val', nombreLargo).trigger('input');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-titular').should('have.class', 'is-invalid');
    });

    it('acepta un titular dentro del rango (5-100 caracteres)', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez'); // 9 caracteres
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');

      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.get('#p-titular').should('not.have.class', 'is-invalid');
    });
  });

  // AGREGADO — RN-CU04-04: el documento exige CVV de 3 a 4 dígitos; el
  // spec original solo probaba que el campo fuera type="password".
  describe('RN-CU04-04: CVV de 3 a 4 dígitos', () => {
    beforeEach(abrirPantallaDePago);

    it('rechaza un CVV de 2 dígitos', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('12');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-cvv').should('have.class', 'is-invalid');
    });

    it('rechaza un CVV de 5 dígitos', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('12345');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-cvv').should('have.class', 'is-invalid');
    });

    it('acepta un CVV de 3 dígitos', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');

      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.get('#p-cvv').should('not.have.class', 'is-invalid');
    });

    it('acepta un CVV de 4 dígitos', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('1234');

      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.get('#p-cvv').should('not.have.class', 'is-invalid');
    });
  });

  describe('FA01 - Validación de campos del formulario fallida', () => {
    beforeEach(abrirPantallaDePago);

    it('NOTA GAP (B): número de tarjeta inválido — mensaje EXACTO del documento', () => {
      cy.get('#p-tarjeta').type('4111111111111112'); // Luhn inválido
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-tarjeta').should('have.class', 'is-invalid');
      // Texto exacto del ejemplo del documento (FA01). La app real agrega
      // la palabra "ingresado".
      cy.get('#msg').should('contain.text', 'El número de tarjeta no es válido');
    });

    it('NOTA GAP (C): tarjeta vencida — mensaje EXACTO del documento', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('0120'); // enero 2020
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-venc').should('have.class', 'is-invalid');
      // Texto exacto del ejemplo del documento (FA01). La app real dice
      // "La tarjeta de crédito/débito se encuentra vencida."
      cy.get('#msg').should('contain.text', 'La tarjeta está vencida');
    });

    // AGREGADO — el documento da un tercer ejemplo de mensaje en FA01,
    // aparte de tarjeta inválida (gap B) y tarjeta vencida (gap C), que
    // faltaba probar: formato de vencimiento incorrecto.
    it('vencimiento con formato incorrecto muestra "Formato inválido. Use MM/AA"', () => {
      cy.get('#p-tarjeta').type(TARJETA_VALIDA);
      cy.get('#p-titular').type('ana lopez');
      // Se fuerza un valor que no calza con el auto-formateo MM/AA.
      cy.get('#p-venc').invoke('val', '13/40').trigger('input');
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();

      cy.get('#p-venc').should('have.class', 'is-invalid');
      // El mensaje específico va debajo del campo (.invalid-feedback), no en
      // el banner general #msg — así lo hace el resto de formularios del
      // sistema (ver registro.html): #msg siempre muestra el mensaje
      // genérico "Revise los campos marcados en rojo.".
      cy.get('#p-venc').next('.invalid-feedback').should('contain.text', 'Formato inválido. Use MM/AA');
      cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo.');
    });

    it('Tras corregir los campos señalados, el paciente puede continuar (paso 4 de FA01)', () => {
      cy.get('#p-tarjeta').type('4111111111111112');
      cy.get('#p-titular').type('ana lopez');
      cy.get('#p-venc').type('1230');
      cy.get('#p-cvv').type('123');
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.get('#p-tarjeta').should('have.class', 'is-invalid');

      cy.get('#p-tarjeta').clear().type(TARJETA_VALIDA);
      cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('pagar');
      cy.contains('button', 'Procesar cargo seguro').click();
      cy.wait('@pagar');

      cy.get('#area-recibo').should('be.visible');
    });
  });

  describe('FA02 - Temporizador de reserva expirado (5 minutos)', () => {
    it('NOTA GAP (D): al expirar, muestra el mensaje EXACTO del documento y redirige al listado', () => {
      cy.clock();
      abrirPantallaDePago();

      cy.tick(5 * 60 * 1000 + 1000);

      // Texto exacto del documento (FA02, paso 2). La app real sí incluye
      // "Será redirigido en unos segundos..." pero omite "su cita",
      // "seleccionado" y la oración "Por favor, seleccione un nuevo
      // horario."
      cy.get('#msg').should('contain.text',
          'El tiempo para confirmar su cita ha expirado. El horario seleccionado ha sido liberado. '
          + 'Por favor, seleccione un nuevo horario. Será redirigido en unos segundos...');
      cy.get('#btn-pagar').should('be.disabled');

      cy.tick(4000);
      cy.get('#area-pago').should('have.class', 'd-none');
      cy.get('#listado').should('not.have.class', 'd-none');
    });
  });

  describe('FA03 - Pago rechazado por la pasarela (contrato del frontend, no del backend real)', () => {
    beforeEach(abrirPantallaDePago);

    // OJO: en los 3 tests de este bloque se simula que el backend YA
    // responde con el texto exacto del documento. El backend real HOY NO
    // lo hace para ninguna de sus 2 tarjetas de prueba (ver NOTA GAP #4 en
    // PagoServiceImplTest.java). Estos tests confirman que, el día que se
    // corrija el backend, el frontend ya sabe mostrar el mensaje y dejar
    // reintentar sin perder la reserva — no confirman el texto real que
    // hoy devuelve el backend.

    const casos = [
      {
        nombre: 'Rechazo bancario',
        mensaje: 'La transacción con tarjeta fue rechazada por el banco. Por favor, verifique los datos '
            + 'de su tarjeta o intente con una tarjeta diferente.',
      },
      {
        nombre: 'Error de procesamiento',
        mensaje: 'El pago no pudo ser procesado. Por favor, intente nuevamente o utilice otra tarjeta.',
      },
      {
        nombre: 'Error de comunicación',
        mensaje: 'Error de comunicación con la pasarela de pago. Intente nuevamente en unos minutos.',
      },
    ];

    casos.forEach(({ nombre, mensaje }) => {
      it(`${nombre}: muestra el mensaje, mantiene el formulario activo y no pierde la reserva`, () => {
        llenarFormularioValido();
        cy.intercept('POST', '/api/pagos', { statusCode: 402, body: { message: mensaje } }).as('pagoRechazado');
        cy.contains('button', 'Procesar cargo seguro').click();
        cy.wait('@pagoRechazado');

        cy.get('#msg').should('contain.text', mensaje);
        cy.get('#area-pago').should('be.visible');
        cy.get('#area-recibo').should('have.class', 'd-none');
        cy.get('#btn-pagar').should('not.be.disabled');
        // El temporizador de reserva sigue corriendo (paso 6 de FA03).
        cy.get('#timer').should('be.visible');

        // El paciente puede corregir y reintentar sin perder el horario.
        cy.get('#p-tarjeta').clear().type(TARJETA_VALIDA);
        cy.intercept('POST', '/api/pagos', { statusCode: 200, body: reciboExitoso }).as('reintento');
        cy.contains('button', 'Procesar cargo seguro').click();
        cy.wait('@reintento');
        cy.get('#area-recibo').should('be.visible');
      });
    });
  });
});