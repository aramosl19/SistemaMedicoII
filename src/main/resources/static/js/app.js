const App = {
    getToken: () => localStorage.getItem('token'),
    getUid: () => localStorage.getItem('uid'),
    getRole: () => localStorage.getItem('rol'),
    getName: () => localStorage.getItem('nombre'),

    logout: () => {
        localStorage.clear();
        window.location.href = 'index.html';
    },

    requireAuth: () => {
        if (!App.getToken()) window.location.href = 'login.html';
    },

    apiFetch: async (endpoint, method = 'GET', body = null, extraHeaders = {}) => {
        const loader = document.getElementById('global-loader');
        if (loader) loader.classList.remove('d-none');

        const token = App.getToken();
        const headers = { 'Content-Type': 'application/json', ...extraHeaders };
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const config = { method, headers };
        if (body) config.body = JSON.stringify(body);

        try {
            const response = await fetch(endpoint, config);
            if (loader) loader.classList.add('d-none');

            let data;
            const contentType = response.headers.get("content-type");
            if (contentType && contentType.includes("application/json")) {
                data = await response.json();
            } else {
                data = { mensaje: "Operación completada." };
            }

            if (response.status === 401 && token) {
                localStorage.clear();
                window.location.href = 'login.html?expired=true';
                return;
            }

            if (!response.ok) {
                let errorMsg = data.error || data.mensaje || 'Ha ocurrido un error en la operación.';
                let fieldErrors = null;
                if (Object.keys(data).length > 0 && !data.error && !data.mensaje) {
                    // El backend (GlobalExceptionHandler) devuelve, para
                    // errores de Bean Validation, un mapa { campo: mensaje }
                    // con un mensaje por cada campo que falló. Antes esto se
                    // aplastaba en un solo string con "errorMsg", perdiendo a
                    // qué campo pertenecía cada mensaje; ahora se conserva el
                    // mapa completo en fieldErrors para poder mostrar cada
                    // mensaje bajo su propio input (ver App.marcarErroresDeCampos).
                    fieldErrors = data;
                    errorMsg = Object.values(data).join(' | ');
                }
                const err = new Error(errorMsg);
                err.fieldErrors = fieldErrors;
                throw err;
            }
            return data;
        } catch (error) {
            if (loader) loader.classList.add('d-none');
            if (error.name === 'TypeError' || error.message === 'Failed to fetch') {
                throw new Error("No se pudo conectar con el servidor. Intente de nuevo más tarde.");
            }
            throw error;
        }
    },
    apiUpload: async (endpoint, file, fieldName = 'archivo') => {
        const loader = document.getElementById('global-loader');
        if (loader) loader.classList.remove('d-none');

        const token = App.getToken();
        const headers = {};
        if (token) headers['Authorization'] = `Bearer ${token}`;

        const formData = new FormData();
        formData.append(fieldName, file);

        try {
            const response = await fetch(endpoint, { method: 'POST', headers, body: formData });
            if (loader) loader.classList.add('d-none');

            let data;
            const contentType = response.headers.get("content-type");
            data = contentType && contentType.includes("application/json")
                ? await response.json()
                : { mensaje: "Operación completada." };

            if (!response.ok) {
                throw new Error(data.error || data.mensaje || 'No se pudo subir el documento.');
            }
            return data;
        } catch (error) {
            if (loader) loader.classList.add('d-none');
            if (error.name === 'TypeError' || error.message === 'Failed to fetch') {
                throw new Error("No se pudo conectar con el servidor. Intente de nuevo más tarde.");
            }
            throw error;
        }
    },

    showAlert: (id, message, isError = false, isWarning = false) => {
        const el = document.getElementById(id);
        if(!el) return;

        if (el._hideTimeout) clearTimeout(el._hideTimeout);

        el.textContent = message;
        el.className = `alert ${isError ? 'alert-error' : (isWarning ? 'alert-warning' : 'alert-success')}`;
        el.style.display = 'block';

        el._hideTimeout = setTimeout(() => {
            el.style.display = 'none';
        }, 5000);
    },

    hideAlert: (id) => {
        const el = document.getElementById(id);
        if(el) el.style.display = 'none';
    },

    // NOTA: se eliminó la capa de validación de formato en el navegador
    // (markInvalidFields/fieldMessages/getValidationMessage, basada en
    // checkValidity()/validity nativos). Toda validación de formato,
    // longitud y campos obligatorios ahora ocurre únicamente en el backend
    // (DTOs con Bean Validation y/o validaciones explícitas en los
    // services). El frontend solo muestra el mensaje de error que el
    // backend devuelve, usando clearInvalidFields/setFieldFeedback/
    // markFieldByErrorMessage a continuación.

    clearInvalidFields: (form) => {
        form.querySelectorAll('.is-invalid').forEach(f => f.classList.remove('is-invalid'));
        form.querySelectorAll('.invalid-feedback').forEach(f => f.textContent = '');
    },

    setFieldFeedback: (campo, mensaje) => {
        let feedback = campo.parentElement.querySelector('.invalid-feedback');
        if (!feedback) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback';
            campo.insertAdjacentElement('afterend', feedback);
        }
        feedback.textContent = mensaje || '';
    },

    // Reemplaza a markFieldByErrorMessage para el caso de varios errores de
    // Bean Validation a la vez (ej. registro con 5 campos inválidos en el
    // mismo submit). markFieldByErrorMessage recibía el mensaje YA
    // concatenado (todos los errores unidos con " | ") y buscaba una sola
    // palabra clave dentro de ese bloque completo; como solo encontraba la
    // primera coincidencia, terminaba pegando el bloque entero de errores
    // bajo un único campo (el bug de "todos los errores debajo de un solo
    // campo"). Esta función en cambio recibe el mapa { campoBackend: mensaje }
    // tal cual lo arma el backend (fieldErrors de apiFetch) y muestra cada
    // mensaje debajo de su propio input, usando mapaCampoBackendAInputId
    // para traducir el nombre del campo del DTO al id del <input> en el HTML.
    // Devuelve { marcoAlgunCampo, mensajesSinCampo }: mensajesSinCampo son
    // los errores cuya clave no estaba en mapaCampoBackendAInputId (ej. si
    // el nombre de un campo del backend no coincide con lo esperado, o si
    // el DTO agrega un campo nuevo y el mapa del HTML no se actualizó). Se
    // devuelven en vez de descartarlos en silencio, para poder mostrarlos
    // igual en el alert general y no perder información del backend.
    marcarErroresDeCampos: (form, fieldErrors, mapaCampoBackendAInputId) => {
        if (!fieldErrors) return { marcoAlgunCampo: false, mensajesSinCampo: [] };
        let marcoAlgunCampo = false;
        const mensajesSinCampo = [];
        for (const campoBackend in fieldErrors) {
            const inputId = mapaCampoBackendAInputId[campoBackend];
            const campo = inputId ? form.querySelector('#' + inputId) : null;
            if (!campo) {
                mensajesSinCampo.push(fieldErrors[campoBackend]);
                continue;
            }
            campo.classList.add('is-invalid');
            App.setFieldFeedback(campo, fieldErrors[campoBackend]);
            marcoAlgunCampo = true;
        }
        return { marcoAlgunCampo, mensajesSinCampo };
    },

    // NOTA: se conserva markFieldByErrorMessage para el caso de un solo
    // mensaje suelto que no viene como mapa por campo (ej. un 409 "ya
    // existe" que el backend manda como { mensaje: "..." } en vez de un
    // mapa de Bean Validation). Para el caso de varios errores a la vez,
    // usar marcarErroresDeCampos (arriba) con err.fieldErrors.
    markFieldByErrorMessage: (form, message, mapaPalabraClave) => {
        if (!message) return null;
        const msgLower = message.toLowerCase();

        if (msgLower.includes('ya existe')) return null;

        for (const palabra in mapaPalabraClave) {
            if (msgLower.includes(palabra)) {
                const campo = form.querySelector('#' + mapaPalabraClave[palabra]);
                if (campo) {
                    campo.classList.add('is-invalid');
                    App.setFieldFeedback(campo, message);
                    return campo;
                }
            }
        }
        return null;
    },

    confirm: function(mensaje, opciones = {}) {
        const titulo = opciones.titulo || '¿Confirmar acción?';
        const textoBoton = opciones.textoBoton || 'Confirmar';
        const peligro = opciones.peligro !== false;

        return new Promise((resolve) => {
            let modal = document.getElementById('app-confirm-modal');
            if (!modal) {
                modal = document.createElement('div');
                modal.id = 'app-confirm-modal';
                modal.className = 'modal-overlay d-none';
                modal.innerHTML = `
                    <div class="modal-content" style="max-width: 420px; text-align: center;">
                        <div class="badge-mark" style="margin: 0 auto 1rem; width: 46px; height: 46px; border-radius: 12px 0 12px 0; background: var(--danger-bg); border: 1px solid rgba(224,137,114,0.3); display:flex; align-items:center; justify-content:center;">
                            <i class="fa-solid fa-triangle-exclamation" style="color: var(--danger);"></i>
                        </div>
                        <h3 id="app-confirm-titulo"></h3>
                        <p class="text-muted" id="app-confirm-mensaje"></p>
                        <div class="d-flex mt-4" style="justify-content: center;">
                            <button type="button" class="btn" id="app-confirm-cancelar">Cancelar</button>
                            <button type="button" class="btn" id="app-confirm-aceptar"></button>
                        </div>
                    </div>`;
                document.body.appendChild(modal);
            }

            modal.querySelector('#app-confirm-titulo').textContent = titulo;
            modal.querySelector('#app-confirm-mensaje').textContent = mensaje;
            const btnAceptar = modal.querySelector('#app-confirm-aceptar');
            const btnCancelar = modal.querySelector('#app-confirm-cancelar');
            btnAceptar.textContent = textoBoton;
            btnAceptar.className = `btn ${peligro ? 'btn-danger' : 'btn-success'}`;
            modal.classList.remove('d-none');

            const cerrar = (resultado) => {
                modal.classList.add('d-none');
                btnAceptar.removeEventListener('click', onAceptar);
                btnCancelar.removeEventListener('click', onCancelar);
                resolve(resultado);
            };
            const onAceptar = () => cerrar(true);
            const onCancelar = () => cerrar(false);
            btnAceptar.addEventListener('click', onAceptar);
            btnCancelar.addEventListener('click', onCancelar);
        });
    },

    cerrarSesionConfirmando: async function() {
        if (await App.confirm("¿Está seguro que desea cerrar sesión?", { titulo: '¿Cerrar sesión?', textoBoton: 'Cerrar sesión', peligro: false })) {
            App.logout();
        }
    },

    renderMenu: () => {
        const role = App.getRole();
        if(!role) return '';
        const r = role.toUpperCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "");
        let html = '';

        if (r === 'ADMINISTRADOR') {
            const enUsuarios = /admin_usuarios\.html|crear_usuario\.html/.test(location.pathname);
            html += `<div class="nav-item-group">`;
            html += `<div class="nav-item-toggle${enUsuarios ? ' open' : ''}" onclick="App.toggleSidebarSubmenu('usuarios')" id="toggle-usuarios">`;
            html += `<span>Usuarios</span><span class="toggle-caret">›</span></div>`;
            html += `<div class="nav-submenu${enUsuarios ? ' open' : ''}" id="submenu-usuarios">`;
            html += `<a href="admin_usuarios.html"${location.pathname.includes('admin_usuarios.html') ? ' class="active"' : ''}>Listar Usuarios</a>`;
            html += `<a href="crear_usuario.html"${location.pathname.includes('crear_usuario.html') ? ' class="active"' : ''}>Crear Usuario</a>`;
            html += `</div></div>`;
            html += `<a href="admin_catalogos.html">Mantenimiento de Catálogos</a>`;
            html += `<a href="bitacora.html">Bitácora de Operación</a>`;
        }
        if (r === 'PACIENTE' || r === 'ADMINISTRADOR') {
            html += `<a href="paciente_agendar.html">Agendar Cita Médica</a>`;
            html += `<a href="paciente_citas.html">Mis Citas y Pagos</a>`;
        }
        if (r === 'RECEPCIONISTA' || r === 'ADMINISTRADOR') {
            html += `<a href="recepcion.html">Recepción de Pacientes</a>`;
        }
        if (r === 'CAJERO' || r === 'ADMINISTRADOR') {
            html += `<a href="caja.html">Caja y Cobros</a>`;
        }
        if (r === 'ENFERMERO' || r === 'ADMINISTRADOR') {
            html += `<a href="enfermeria.html">Toma de Signos Vitales</a>`;
        }
        if (r === 'MEDICO' || r === 'ADMINISTRADOR') {
            html += `<a href="medico_panel.html">Panel de Consultas</a>`;
            html += `<a href="medico_agenda.html">Agenda Médica</a>`;
            html += `<a href="medico_resultados_laboratorio.html">Resultados de Laboratorio</a>`;
        }
        if (r === 'LABORATORISTA' || r === 'SUPERVISORLABORATORIO' || r === 'ADMINISTRADOR') {
            html += `<a href="laboratorio.html">Área de Laboratorio</a>`;
        }
        if (r === 'FARMACEUTICO' || r === 'ADMINISTRADOR') {
            html += `<a href="farmacia_despacho.html">Despacho de Farmacia</a>`;
            html += `<a href="farmacia_inventario.html">Bitácora de Inventario</a>`;
        }

        html += `<div style="margin-top: 2rem; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 1rem;">`;
        html += `<a href="dashboard.html" style="color: #93c5fd; font-weight: 500;">Volver al Inicio</a>`;
        html += `<a href="#" onclick="App.cerrarSesionConfirmando(); return false;" style="color: #fca5a5; font-weight: 500;"> Cerrar Sesión</a>`;
        html += `</div>`;

        return html;
    },

    toggleSidebarSubmenu: (nombre) => {
        document.getElementById(`toggle-${nombre}`).classList.toggle('open');
        document.getElementById(`submenu-${nombre}`).classList.toggle('open');
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('sidebar-menu');
    if(sidebar) sidebar.innerHTML = App.renderMenu();

    if (!document.getElementById('global-loader')) {
        const loaderDiv = document.createElement('div');
        loaderDiv.id = 'global-loader';
        loaderDiv.className = 'd-none';
        loaderDiv.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(loaderDiv);
    }
});