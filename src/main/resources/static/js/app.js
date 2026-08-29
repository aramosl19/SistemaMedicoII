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
                if (Object.keys(data).length > 0 && !data.error && !data.mensaje) {
                    errorMsg = Object.values(data).join(' | ');
                }
                throw new Error(errorMsg);
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

    markInvalidFields: (form) => {
        const invalidos = [];
        form.querySelectorAll('input, select, textarea').forEach(field => {
            if (field.willValidate && !field.checkValidity()) {
                field.classList.add('is-invalid');
                App.setFieldFeedback(field, App.getValidationMessage(field));
                invalidos.push(field);
            } else {
                field.classList.remove('is-invalid');
                App.setFieldFeedback(field, '');
            }
        });
        return invalidos;
    },

    fieldMessages: {
        nombre: {
            required: 'El nombre debe contener entre 10 y 100 caracteres.',
            tooShort: (len) => `El nombre debe contener entre 10 y 100 caracteres. Usted ingresó ${len} caracteres.`,
            tooLong: (len) => `El nombre debe contener entre 10 y 100 caracteres. Usted ingresó ${len} caracteres.`
        },
        dpi: {
            required: 'El campo DPI es obligatorio. Por favor, ingrese su número de DPI.',
            patternMismatch: 'El DPI debe contener únicamente números. No se permiten letras ni caracteres especiales.',
            tooShort: (len) => `El DPI debe contener exactamente 13 dígitos. Usted ingresó ${len} dígitos.`,
            tooLong: (len) => `El DPI debe contener exactamente 13 dígitos. Usted ingresó ${len} dígitos.`
        },
        nit: {
            required: 'El campo NIT es obligatorio.',
            tooShort: (len) => `El NIT debe contener entre 8 y 9 caracteres. Usted ingresó ${len} caracteres.`,
            tooLong: (len) => `El NIT debe contener entre 8 y 9 caracteres. Usted ingresó ${len} caracteres.`,
            patternMismatch: 'El NIT debe contener únicamente caracteres alfanuméricos.'
        },
        telefono: {
            required: 'El número de teléfono debe contener exactamente 8 dígitos numéricos.',
            patternMismatch: 'El número de teléfono debe contener exactamente 8 dígitos numéricos.'
        },
        seguro: {
            tooShort: () => 'El número de seguro debe contener entre 5 y 50 caracteres.',
            tooLong: () => 'El número de seguro debe contener entre 5 y 50 caracteres.'
        },
        correo: {
            required: 'El campo Correo es obligatorio.',
            typeMismatch: 'El formato del correo electrónico no es válido. Ejemplo: usuario@dominio.com'
        },
        username: {
            required: 'El usuario debe contener al menos 8 caracteres.',
            tooShort: () => 'El usuario debe contener al menos 8 caracteres.',
            tooLong: () => 'El usuario no puede exceder los 9 caracteres.',
            patternMismatch: 'El usuario debe contener únicamente caracteres alfanuméricos.'
        },
        password: {
            required: 'La contraseña debe contener al menos 12 caracteres.',
            tooShort: () => 'La contraseña debe contener al menos 12 caracteres.'
        },
        'p-venc': {
            required: 'La fecha de vencimiento es obligatoria.',
            patternMismatch: 'Formato inválido. Use MM/AA'
        },
        diag: {
            required: 'El diagnóstico es obligatorio para finalizar la consulta.',
            tooShort: (len) => `El diagnóstico debe contener entre 10 y 5000 caracteres. Usted ingresó ${len} caracteres.`,
            tooLong: (len) => `El diagnóstico debe contener entre 10 y 5000 caracteres. Usted ingresó ${len} caracteres.`
        }
    },

    getValidationMessage: (field) => {
        const v = field.validity;
        const len = field.value.length;
        const cfg = App.fieldMessages[field.id];
        if (cfg) {
            if (v.valueMissing && cfg.required) return cfg.required;
            if (v.tooShort && cfg.tooShort) return cfg.tooShort(len);
            if (v.tooLong && cfg.tooLong) return cfg.tooLong(len);
            if (v.patternMismatch && cfg.patternMismatch) return cfg.patternMismatch;
            if (v.typeMismatch && cfg.typeMismatch) return cfg.typeMismatch;
        }
        return field.validationMessage;
    },

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