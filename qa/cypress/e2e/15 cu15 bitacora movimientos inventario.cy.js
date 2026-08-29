// Ubicación sugerida: qa/cypress/e2e/15_CU15_bitacora_movimientos_inventario.cy.js
//
// Este spec está escrito CONTRA EL DOCUMENTO "15_CU_Bitacora_Movimientos_Inventario.docx"
// (que internamente se autotitula "CU-13 Bitácora de Movimientos de Inventario") y contra
// el código real de MovimientoInventarioController / MovimientoInventarioServiceImpl /
// MovimientoInventarioRequestDTO / farmacia_inventario.html. Donde el código se desvía del
// texto/las reglas del documento, la prueba queda TAL CUAL el documento lo exige y por lo
// tanto VA A FALLAR -- con un comentario "NOTA GAP" explicando la diferencia encontrada. No
// se ajustó ningún selector, texto esperado ni mock para maquillar el resultado.
//
// AVISO DE NUMERACIÓN (léelo antes de nombrar el archivo definitivo):
// El documento se autotitula "CU-13" en su propio encabezado, y coincide en ese número con
// Reglas_de_Negocio_Consolidadas.docx (donde CU-13 = "Bitácora de Movimientos de Inventario").
// PERO el archivo que me diste se llama "15_CU_Bitacora_...docx", y el código YA está
// comentado en más de diez lugares como "Solución CU-15 (gap #N del QA)". Además ya existe
// un spec "14_CU13_configuracion_sedes_especialidades.cy.js" que ocupa el número CU-13 para
// un caso de uso totalmente distinto. Dejé el describe() y el nombre de archivo en "CU-15"
// por ser lo que ya está reflejado en el código, pero confirmá cuál es el número real antes
// de la entrega final -- es el mismo tipo de duda ya dejada pendiente en specs anteriores.
//
// AVISO DE FUENTE EN CONFLICTO:
// Reglas_de_Negocio_Consolidadas.docx trae una versión distinta y más antigua de
// RN-CU13-01/02 (tipos "Entrada por compra/Salida por ajuste/Transferencia/Ajuste físico",
// motivo siempre obligatorio 10-1000 caracteres, validación de stock solo para "Salida o
// Transferencia"). Esa versión NO coincide con el CU-13.docx individual (tipos 0-6,
// mensajes con placeholders, reglas de motivo dinámicas por tipo) ni con el código real,
// que sí están alineados entre sí. Este spec usa el CU-13.docx individual como fuente de
// verdad, tal como se pidió. Las Reglas de Negocio Consolidadas quedan desactualizadas
// respecto a esto -- avisar para que se sincronicen los documentos.
//
// ============================= RESUMEN DE GAPS =============================
//   A) [Paso 2] Encabezados de columna de la tabla no calzan con el texto del documento:
//      dice "Sede" en vez de "Sucursal", "Item" en vez de "Medicamento", "Documento" en vez
//      de "Referencia", "Operación" en vez de "Tipo". Todos los datos exigidos SÍ están
//      presentes, solo cambia el rótulo visible.
//   B) [FA01] El documento pide el mensaje genérico del "componente TableServer": "No se
//      encontraron resultados." (ese texto exacto SÍ se usa en otra pantalla del mismo
//      proyecto, ej. admin_catalogos.html). Esta página usa un texto distinto: "No hay
//      movimientos que coincidan con la búsqueda." -- rompe el patrón ya establecido en el
//      resto del sistema, además de no calzar con el documento.
//   C) [Paso 3] El dropdown de Tipo de Movimiento no usa el texto exacto del documento
//      (Compra/Devolución/Venta/Reclamo/Ajuste+/Ajuste-). Usa en su lugar "Ingreso por
//      compra", "Devolución a proveedor", "Venta directa (mostrador)", "Reclamo / mermas",
//      "Ajuste positivo (+)", "Ajuste negativo (-)".
//   D) [Paso 3] La etiqueta dinámica de "Número de Referencia" no usa el texto exacto que
//      pide el documento (Factura/Devolución/Venta/Reclamo). Usa "No. de factura", "No.
//      documento devolución", "No. recibo venta", "ID reclamo".
//   E) [Paso 3] La etiqueta de Motivo/Notas para Reclamo dice "Detalle del reclamo" en vez
//      de "Motivo del Reclamo" (texto exacto del documento). Para Ajuste+/Ajuste- dice
//      "Justificación del ajuste" en vez de "Notas/Justificación" (texto exacto del
//      documento). El campo de Devolución sí calza salvo por mayúscula inicial ("Motivo de
//      devolución" vs "Motivo de Devolución" del documento).
//   F) [Paso 5] El botón de envío del formulario dice "Afectar kardex"; el documento pide
//      que el usuario seleccione "Registrar Movimiento". Desviación de texto, no de
//      comportamiento.
//   G) [RESUELTO] Costo Unitario ahora valida sus 3 reglas por separado en el backend, y
//      el <input id="co"> perdió su required/step nativo para que esas 3 reglas siempre
//      lleguen al backend (antes, la validación nativa del navegador bloqueaba el submit
//      antes de que el backend pudiera responder con el mensaje específico).
//   H) [RESUELTO] Cantidad ahora usa un solo mensaje ("La cantidad debe ser un número
//      entero positivo.") tanto en @NotNull como en @Min.
//   M) [RESUELTO - bug nuevo encontrado al correr este spec] verificarStock() calculaba la
//      variable "operacion" (Entrada/Salida) pero nunca la insertaba en el HTML del panel;
//      el panel jamás mostraba el tipo de operación que pide RN-CU13-03. Ya se agrega al
//      panel.
//   I) [FA03] El documento pide que "el sistema resalta los campos en rojo" ante una
//      validación fallida. El mecanismo para esto YA EXISTE en el proyecto
//      (App.markInvalidFields / App.markFieldByErrorMessage / clase "is-invalid" + mensaje
//      bajo el campo, usado en CU-02 registro de pacientes) pero NUNCA se invoca en
//      farmacia_inventario.html: el formulario solo confía en la validación nativa del
//      navegador y, ante un error del backend, únicamente muestra un alert genérico.
//   J) [FA04] El flujo de "Cancelar" no existe en absoluto: no hay ningún botón "Cancelar"
//      en el formulario, por lo que no hay forma de descartar los datos y volver al listado
//      como pide el documento.
//   K) [Paso 2, Acciones] El documento pide que "Ver" "navega a detalle" (sugiere una vista
//      separada). La implementación abre un modal en la misma página en vez de navegar.
//      Podría ser una decisión de diseño válida (ya se usó el mismo patrón de modal en
//      CU-09) -- se deja marcado para que confirmes si es gap real o decisión de negocio,
//      sin test que lo bloquee.
//   L) [RN-CU13-03] El documento menciona "Bitácora embebida en MedicineInventoryPage con
//      resumen mensual de movimientos". No existe ninguna página ni componente llamado
//      "MedicineInventoryPage" en este proyecto (nombre que no encaja con la convención de
//      nombres del resto del HTML estático), y el resumen mensual no está "embebido": se
//      abre bajo demanda en un modal vía el botón "Reporte Mensual". Podría ser texto de
//      plantilla no adaptado a esta implementación -- sin test, solo nota para que lo
//      revises.
//
// ========================= CONFIRMADO CORRECTO (sin gap) =========================
//   - Paso 8 / mensaje de éxito "Movimiento registrado exitosamente. Medicamento: [nombre].
//     Tipo: [tipo]. Cantidad: [cantidad]. Stock actualizado: [nuevo stock]." -- coincide
//     literal.
//   - FA02 mensaje de stock insuficiente "Stock insuficiente. Stock actual: [cantidad]. No
//     se puede registrar una salida de [cantidad solicitada] unidades." -- coincide literal
//     en el backend.
//   - FA05 mensaje de alerta preventiva "[nombre]: Stock bajo — disponible: [cantidad]
//     (mínimo: [X])" -- coincide literal.
//   - RN-CU13-02 control de concurrencia optimista -- confirmado: InventarioMedicamento
//     tiene campo @Version.
//   - RN-CU13-01 tipo 6 (Despacho) correctamente excluido del formulario de creación manual,
//     y el backend rechaza explícitamente su creación manual.
//   - RN-CU13-01 Venta: el campo Motivo/Notas correctamente NO se muestra.
//   - Filtros de tabla: existen los 6 filtros que pide el documento (Tipo, Medicamento,
//     Sucursal, Referencia, Usuario, rango de fechas) -- todos presentes y funcionales,
//     aunque con placeholders de texto distinto al documento (no se consideró gap porque el
//     documento no exige un placeholder literal, solo que el filtro exista).
//   - Panel informativo de inventario en tiempo real (stock actual, mínimo, proyectado, tipo
//     de operación) -- presente y correcto.

describe('CU-15 - Bitácora de Movimientos de Inventario', () => {

    const sucursales = [{ id: 1, nombre: 'Sede Central', activo: true }];
    const medicamentos = [
        { id: 10, nombre: 'Paracetamol 500mg', activo: true, precio: 2.50 },
        { id: 11, nombre: 'Amoxicilina 500mg', activo: true, precio: 3.00 },
    ];
    const inventario = [
        { id: 1, medicamentoId: 10, medicamentoNombre: 'Paracetamol 500mg', sucursalId: 1, sucursalNombre: 'Sede Central', stockActual: 20, stockMinimo: 10, alertaStockBajo: false, medicamentoControlado: false, activo: true },
        { id: 2, medicamentoId: 11, medicamentoNombre: 'Amoxicilina 500mg', sucursalId: 1, sucursalNombre: 'Sede Central', stockActual: 5, stockMinimo: 10, alertaStockBajo: true, medicamentoControlado: false, activo: true },
    ];
    const movimientoRegistrado = {
        id: 501, tipoMovimientoNombre: 'Compra', medicamentoNombre: 'Paracetamol 500mg',
        sucursalNombre: 'Sede Central', cantidad: 50, stockAnterior: 20, stockNuevo: 70,
        costoUnitario: 2.00, referencia: 'FAC-001', motivo: 'Reposición mensual',
        fechaHora: '2026-08-20T10:15:00', usuarioNombre: 'flopez', activo: true,
    };

    const abrirModulo = (movimientos = [movimientoRegistrado]) => {
        cy.simularSesion({ rol: 'Farmaceutico', nombre: 'Lic. Marta Solís', uid: 70 });
        cy.intercept('GET', '/api/sucursales', sucursales).as('sucursales');
        cy.intercept('GET', '/api/medicamentos', medicamentos).as('medicamentos');
        cy.intercept('GET', '/api/usuarios/me', { sucursalId: 1 }).as('me');
        cy.intercept('GET', '/api/inventario', inventario).as('inventario');
        cy.intercept('GET', '/api/inventario/movimientos', movimientos).as('movimientos');
        cy.visit('/farmacia_inventario.html');
        cy.wait(['@sucursales', '@medicamentos', '@me', '@inventario', '@movimientos']);
    };

    it('Paso 2: la tabla de movimientos usa las columnas exactas del documento', () => {
        // NOTA GAP A: la tabla real usa "Sede", "Item", "Documento" y "Operación" en vez de
        // "Sucursal", "Medicamento", "Referencia" y "Tipo". Este test queda tal como lo pide
        // el documento y por lo tanto falla contra los encabezados reales.
        abrirModulo();
        // (confirmado correcto tras cambiar "Cant." por "Cantidad" en el <thead>)
        const encabezados = ['Tipo', 'Medicamento', 'Sucursal', 'Cantidad', 'Stock Anterior', 'Stock Nuevo', 'Costo', 'Referencia', 'Usuario', 'Fecha'];
        encabezados.forEach(texto => {
            cy.get('table.table thead th').should('contain.text', texto);
        });
    });

    it('FA01: sin movimientos que coincidan, muestra el mensaje genérico "No se encontraron resultados."', () => {
        // NOTA GAP B: el mensaje real es "No hay movimientos que coincidan con la búsqueda.",
        // distinto del texto exacto que pide el documento (y usado en otras pantallas del
        // mismo sistema).
        abrirModulo([]);
        cy.get('#tb').should('contain.text', 'No se encontraron resultados.');
    });

    it('Paso 2: acciones de fila son "Ver" (navega a detalle) y "Desactivar/Activar"', () => {
        abrirModulo();
        cy.contains('#tb button', 'Ver').should('exist');
        cy.contains('#tb button', 'Desactivar').should('exist');
    });

    it('Paso 3: el dropdown de Tipo de Movimiento usa el texto exacto del documento', () => {
        // NOTA GAP C: las opciones reales son "Ingreso por compra", "Devolución a
        // proveedor", "Venta directa (mostrador)", "Reclamo / mermas", "Ajuste positivo
        // (+)" y "Ajuste negativo (-)". Ninguna calza con el texto literal del documento.
        abrirModulo();
        const opciones = ['Compra', 'Devolución', 'Venta', 'Reclamo', 'Ajuste+', 'Ajuste-'];
        opciones.forEach(texto => {
            cy.get('#t option').should('contain.text', texto);
        });
    });

    it('Paso 3: la etiqueta de Número de Referencia cambia según el tipo (texto del documento)', () => {
        // NOTA GAP D: las etiquetas reales son "No. de factura", "No. documento
        // devolución", "No. recibo venta" e "ID reclamo" -- ninguna es el texto plano
        // (Factura/Devolución/Venta/Reclamo) que pide el documento.
        abrirModulo();
        cy.get('#t').select('0'); // Compra
        cy.get('#lbl-ref').should('have.text', 'Factura');
        cy.get('#t').select('1'); // Devolución
        cy.get('#lbl-ref').should('have.text', 'Devolución');
        cy.get('#t').select('2'); // Venta
        cy.get('#lbl-ref').should('have.text', 'Venta');
        cy.get('#t').select('3'); // Reclamo
        cy.get('#lbl-ref').should('have.text', 'Reclamo');
    });

    it('Paso 3 / RN-CU13-01: Motivo obligatorio con la etiqueta exacta según tipo', () => {
        // NOTA GAP E: Reclamo usa "Detalle del reclamo" (doc pide "Motivo del Reclamo") y
        // Ajuste+/Ajuste- usan "Justificación del ajuste" (doc pide "Notas/Justificación").
        abrirModulo();
        cy.get('#t').select('1'); // Devolución
        cy.get('#lbl-mot').should('have.text', 'Motivo de Devolución');
        cy.get('#mo').should('have.attr', 'required');
        cy.get('#t').select('3'); // Reclamo
        cy.get('#lbl-mot').should('have.text', 'Motivo del Reclamo');
        cy.get('#t').select('4'); // Ajuste+
        cy.get('#lbl-mot').should('have.text', 'Notas/Justificación');
    });

    it('Venta: el campo Motivo/Notas no se muestra (confirmado correcto)', () => {
        abrirModulo();
        cy.get('#t').select('2'); // Venta
        cy.get('#div-motivo').should('have.class', 'd-none');
    });

    it('Paso 3: el panel de inventario en tiempo real muestra stock actual, mínimo, proyectado y tipo de operación', () => {
        abrirModulo();
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Paracetamol 500mg');
        cy.get('#t').select('0'); // Compra -> entrada
        cy.get('#c').type('10');
        cy.get('#panel-stock').should('be.visible')
            .and('contain.text', 'Stock actual: 20')
            .and('contain.text', 'Stock mínimo: 10')
            .and('contain.text', 'Entrada')
            .and('contain.text', '30');
        // (confirmado correcto tras agregar "operacion" al HTML del panel -- gap M)
    });

    it('FA05: alerta preventiva de stock bajo con el texto exacto del documento (confirmado correcto)', () => {
        abrirModulo();
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Amoxicilina 500mg'); // stock 5, mínimo 10
        cy.get('#t').select('0'); // entrada, pero seguirá bajo mínimo si compramos poco
        cy.get('#c').type('2'); // proyectado = 7, sigue < 10 y >= 0
        cy.get('#panel-stock').should('contain.text', 'Amoxicilina 500mg: Stock bajo — disponible: 7 (mínimo: 10)');
    });

    it('Paso 5: el botón para registrar dice "Registrar Movimiento"', () => {
        // NOTA GAP F: el botón real dice "Afectar kardex".
        abrirModulo();
        cy.contains('#f-inv button[type="submit"]', 'Registrar Movimiento').should('exist');
    });

    it('Paso 8: mensaje de éxito con el texto exacto del documento (confirmado correcto)', () => {
        abrirModulo();
        cy.intercept('POST', '/api/inventario/movimientos', { statusCode: 201, body: movimientoRegistrado }).as('registrar');
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Paracetamol 500mg');
        cy.get('#t').select('0');
        cy.get('#c').type('50');
        cy.get('#co').type('2.00');
        cy.get('#f-inv').submit();
        cy.wait('@registrar');
        cy.get('#msg').should('contain.text', 'Movimiento registrado exitosamente. Medicamento: Paracetamol 500mg. Tipo: Compra. Cantidad: 50. Stock actualizado: 70.');
    });

    it('FA02: stock insuficiente muestra el mensaje exacto del documento (confirmado correcto)', () => {
        abrirModulo();
        cy.intercept('POST', '/api/inventario/movimientos', {
            statusCode: 400,
            body: { error: 'Stock insuficiente. Stock actual: 5. No se puede registrar una salida de 10 unidades.' },
        }).as('registrar');
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Amoxicilina 500mg');
        cy.get('#t').select('2'); // Venta
        cy.get('#c').type('10');
        cy.get('#f-inv').submit();
        cy.wait('@registrar');
        cy.get('#msg').should('contain.text', 'Stock insuficiente. Stock actual: 5. No se puede registrar una salida de 10 unidades.');
    });

    it('RN-CU13-01: Costo Unitario obligatorio y mayor a 0 solo para Compra, con mensajes específicos (confirmado correcto tras quitar la validación nativa del input)', () => {
        // El campo #co ya no tiene required/step nativos: las 3 reglas las resuelve el
        // backend y el mensaje literal aparece en el .invalid-feedback debajo del campo
        // (mismo patrón usado en CU-02), mientras que #msg muestra el texto genérico.
        abrirModulo();
        cy.intercept('POST', '/api/inventario/movimientos', {
            statusCode: 400,
            body: { error: 'El costo unitario es obligatorio para compras' },
        }).as('sinCosto');
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Paracetamol 500mg');
        cy.get('#t').select('0');
        cy.get('#c').type('10');
        cy.get('#f-inv').submit();
        cy.wait('@sinCosto');
        cy.get('#co').next('.invalid-feedback').should('have.text', 'El costo unitario es obligatorio para compras');
        cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');

        cy.intercept('POST', '/api/inventario/movimientos', {
            statusCode: 400,
            body: { error: 'El costo unitario debe tener máximo 2 decimales' },
        }).as('decimales');
        cy.get('#co').type('2.999');
        cy.get('#f-inv').submit();
        cy.wait('@decimales');
        cy.get('#co').next('.invalid-feedback').should('have.text', 'El costo unitario debe tener máximo 2 decimales');
    });

    it('RN-CU13-01: Cantidad obligatoria, entero positivo, con el mensaje único del documento', () => {
        // NOTA GAP H: cuando Cantidad viene vacía, el backend responde con el mensaje de
        // @NotNull ("La cantidad es obligatoria."), distinto del único mensaje que pide el
        // documento para todo el campo.
        abrirModulo();
        cy.intercept('POST', '/api/inventario/movimientos', {
            statusCode: 400,
            body: { error: 'La cantidad debe ser un número entero positivo.' },
        }).as('cantidadInvalida');
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Paracetamol 500mg');
        cy.get('#t').select('1'); // Devolución, no requiere costo
        cy.get('#mo').type('Producto vencido antes de vender');
        // El navegador bloquea el submit con cantidad vacía por el atributo required nativo;
        // se fuerza el envío para simular lo que respondería el backend según el documento.
        cy.get('#c').invoke('removeAttr', 'required');
        cy.get('#f-inv').submit();
        cy.wait('@cantidadInvalida');
        // El mensaje literal va en el .invalid-feedback debajo del campo (mismo patrón de
        // CU-02); #msg solo muestra el texto genérico "Revise los campos marcados en rojo".
        cy.get('#c').next('.invalid-feedback').should('have.text', 'La cantidad debe ser un número entero positivo.');
        cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    it('FA03: validación fallida resalta los campos en rojo (mecanismo ya usado en CU-02, no conectado aquí)', () => {
        // NOTA GAP I: App.markInvalidFields / App.markFieldByErrorMessage existen en
        // js/app.js y agregan la clase "is-invalid" + un mensaje bajo el campo (patrón usado
        // en el registro de pacientes), pero el formulario de esta página nunca los invoca.
        abrirModulo();
        cy.intercept('POST', '/api/inventario/movimientos', {
            statusCode: 400,
            body: { error: 'El motivo debe contener entre 10 y 500 caracteres.' },
        }).as('motivoInvalido');
        cy.get('#s').select('Sede Central');
        cy.get('#m').select('Paracetamol 500mg');
        cy.get('#t').select('4'); // Ajuste+
        cy.get('#c').type('5');
        cy.get('#mo').invoke('removeAttr', 'minlength'); // dejar pasar la validación nativa para llegar al backend
        cy.get('#mo').type('corto');
        cy.get('#f-inv').submit();
        cy.wait('@motivoInvalido');
        cy.get('#mo').should('have.class', 'is-invalid');
        cy.get('#msg').should('contain.text', 'Revise los campos marcados en rojo');
    });

    it('FA04: existe un botón "Cancelar" que descarta los datos y vuelve al listado', () => {
        // NOTA GAP J: no existe ningún botón "Cancelar" en el formulario; el flujo alterno
        // completo no está implementado.
        abrirModulo();
        cy.contains('#f-inv button', 'Cancelar').should('exist');
    });
});