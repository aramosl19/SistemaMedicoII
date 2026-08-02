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

            // FIX: solo tratamos 401/403 como "sesión expirada" si la petición
            // llevaba un token (es decir, era una llamada ya autenticada).
            // Si no había token (ej. el propio login fallando), dejamos que
            // el error real llegue al catch de quien llamó.
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
                invalidos.push(field);
            } else {
                field.classList.remove('is-invalid');
            }
        });
        return invalidos;
    },

    clearInvalidFields: (form) => {
        form.querySelectorAll('.is-invalid').forEach(f => f.classList.remove('is-invalid'));
    },

    // Modal de confirmación reutilizable en todo el sistema
    confirm: function(mensaje, opciones = {}) {
        const titulo = opciones.titulo || '¿Confirmar acción?';
        const textoBoton = opciones.textoBoton || 'Confirmar';
        const peligro = opciones.peligro !== false; // rojo por defecto, pasa peligro:false para verde

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
            html += `<a href="admin_usuarios.html">Mantenimiento de Usuarios</a>`;
            html += `<a href="admin_catalogos.html">Mantenimiento de Catálogos</a>`;
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
            // FIX QA (gap #10 verificación con Edy — CU-09): no existía ninguna pantalla
            // donde el médico pudiera consultar resultados de laboratorio publicados.
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