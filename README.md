# SistemaMedicoII

Backend (API REST) de un sistema de gestión hospitalaria para **Hospital El Milagro**. Cubre el flujo completo de la atención: agendamiento y pago de citas, recepción, caja, enfermería, consulta médica, laboratorio, farmacia y administración.

> **Estado:** el proyecto está **publicado en Azure, junto con su base de datos PostgreSQL**, y cuenta con **CI/CD** en GitHub Actions: con cada push a `main` construye y actualiza la imagen del backend en Azure (el arranque del sistema en Azure no es automático). Más detalle en [Despliegue en Azure y CI/CD](#despliegue-en-azure-y-cicd).

---

## Tabla de contenido

1. [Casos de uso](#casos-de-uso)
2. [Tecnologías](#tecnologías)
3. [Roles del sistema](#roles-del-sistema)
4. [Requisitos](#requisitos)
5. [Configuración](#configuración)
6. [Cómo ejecutarlo](#cómo-ejecutarlo)
7. [Datos de prueba](#datos-de-prueba)
8. [Estructura del proyecto](#estructura-del-proyecto)
9. [Decisiones de diseño](#decisiones-de-diseño)
10. [SOLID y OWASP](#solid-y-owasp)
11. [Pruebas](#pruebas)
12. [Despliegue en Azure y CI/CD](#despliegue-en-azure-y-cicd)
13. [Herramientas de desarrollo](#herramientas-de-desarrollo)
14. [Autor](#autor)

---

## Casos de uso
Como detalle los coloque en la carpeta "docs" en la raiz del proyecto.

| CU | Nombre |
|----|--------|
| CU-00 | Visualización del portal web (verificación de DPI e inicio de sesión) |
| CU-01 | Mantenimiento de usuarios |
| CU-02 | Registro de paciente |
| CU-03 | Agendar cita (con documento PDF adjunto) |
| CU-04 | Pago en línea |
| CU-05 | Recepción y verificación de cita |
| CU-06 | Cobro de consulta en caja |
| CU-07 | Toma de signos vitales |
| CU-08 | Consulta médica (diagnóstico CIE-10 y receta) |
| CU-09 | Gestión de laboratorio |
| CU-10 | Cobro de laboratorio en caja |
| CU-11 | Despacho de medicamentos |
| CU-12 | Agendamiento de cita de seguimiento |
| CU-13 | Configuración de sedes y especialidades |
| CU-15 | Bitácora y movimientos de inventario |
| CU-16 | Gestión de agenda médica |

---

## Tecnologías

**Backend**

- Java 17 y Spring Boot 4.1.0
- Spring Web, Spring Data JPA, Spring Security y Bean Validation
- PostgreSQL (alojada en Azure en el entorno publicado)
- JWT para autenticación (jjwt 0.12.6)
- BCrypt para contraseñas y cifrado AES para datos sensibles (DPI y NIT)
- Spring Mail (SMTP) para correos automáticos
- Spring Cache y programación de tareas (`@Scheduled`)
- Spring AOP para auditoría de operaciones
- Azure Blob Storage para documentos adjuntos
- PDFBox para validar el contenido de los PDF y ClamAV para el escaneo antivirus
- Lombok y Maven

**Pruebas**

- Postman para probar la API manualmente antes de crear el frontend
- JUnit y Mockito (pruebas unitarias y de controladores)
- Cypress para pruebas end to end (ver aviso en la sección de pruebas)

**Despliegue en la nube**

- Docker, GitHub Actions (CI/CD), Azure Container Registry y Azure Container Apps, con la base de datos PostgreSQL alojada en Azure

---

## Roles del sistema

| Rol | Qué hace |
|-----|----------|
| `PACIENTE` | Agenda y paga sus citas |
| `RECEPCIONISTA` | Verifica la llegada de pacientes y gestiona citas |
| `CAJERO` | Cobra consultas y exámenes de laboratorio |
| `ENFERMERO` | Registra signos vitales |
| `MEDICO` | Atiende consultas, receta, ordena exámenes y administra su agenda |
| `LABORATORISTA` / `SUPERVISORLABORATORIO` | Gestionan órdenes y resultados de laboratorio |
| `FARMACEUTICO` | Despacha medicamentos y controla el inventario |
| `ADMINISTRADOR` | Usuarios, catálogos, sedes, especialidades y bitácora |

---

## Requisitos

- JDK 17
- Maven 3.9 o superior (o el wrapper incluido en `.mvn/`)
- PostgreSQL con una base de datos llamada `sistema_medico`
- Node.js (solo para correr las pruebas de Cypress)
- Docker (solo para construir la imagen)

---

## Configuración

La configuración está en `src/main/resources/application.properties` y se sobreescribe con variables de entorno:

| Variable | Para qué sirve | Valor por defecto |
|----------|----------------|-------------------|
| `DB_URL` | URL de conexión a PostgreSQL | `jdbc:postgresql://localhost:5432/sistema_medico` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | valor de desarrollo |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Cuenta SMTP (Gmail) para enviar correos | sin valor |
| `ENCRYPTION_KEY` | Clave AES para cifrar DPI y NIT | valor de desarrollo |
| `JWT_SECRET` | Secreto para firmar los tokens JWT | valor de desarrollo |
| `AZURE_STORAGE_CONNECTION_STRING` | Cuenta de Blob Storage para los PDF adjuntos | sin valor |
| `CLAMAV_HOST` / `CLAMAV_PORT` | Servidor ClamAV para el escaneo antivirus | `localhost` / `3310` |

> **Importante:** los valores por defecto de `ENCRYPTION_KEY`, `JWT_SECRET` y `DB_PASSWORD` son solo para desarrollo local. En cualquier entorno publicado hay que definirlos con valores propios. Si se cambia `ENCRYPTION_KEY` después de guardar datos, los DPI y NIT ya cifrados dejan de poder leerse.

Otros ajustes relevantes: zona horaria `America/Guatemala`, límite de 2 MB para el PDF adjunto y token JWT con vigencia de 24 horas.

---

## Cómo ejecutarlo

1. Crear la base de datos:

   ```sql
   CREATE DATABASE sistema_medico;
   ```

2. Definir las variables de entorno que haga falta (al menos las del correo si se quieren probar las notificaciones).

3. Levantar el backend desde la raíz del proyecto:

   ```bash
   mvn spring-boot:run
   ```

   Hibernate crea las tablas automáticamente (`ddl-auto=update`).

4. El backend queda disponible en `http://localhost:8080` (endpoints bajo `/api/...`).

Para que funcione el adjunto de PDF en el agendamiento (CU-03) se necesita un servidor ClamAV accesible y una cuenta de Azure Blob Storage configurada.

---

## Datos de prueba

- Los roles, sedes, especialidades y demás catálogos se administran con los endpoints de administración (`/api/roles`, `/api/sucursales`, `/api/especialidades`, etc.).
- El **DPI y el NIT se guardan cifrados** (AES), por eso los usuarios que necesiten DPI deben crearse con los endpoints de registro de paciente o de mantenimiento de usuarios, y no con un `INSERT` directo en SQL.
- Los usuarios internos que no necesitan DPI (por ejemplo, médicos de prueba) sí pueden insertarse por SQL, siempre con la contraseña ya hasheada con BCrypt.

---

## Estructura del proyecto

```text
SistemaMedicoII/
├── .github/workflows/deploy.yml     # Pipeline de despliegue a Azure
├── qa/
│   └── cypress/                     # Pruebas end to end (un spec por caso de uso)
├── src/main/java/org/umg/sistemamedicoii/
│   ├── aop/                         # Auditoría de operaciones (@Auditable)
│   ├── config/                      # Seguridad, JWT, cifrado y caché
│   ├── controller/                  # API REST agrupada por módulo
│   ├── dto/                         # Objetos de entrada y salida
│   ├── enums/                       # Estados y tipos de concepto
│   ├── exception/                   # Excepciones y manejador global
│   ├── models/                      # Entidades JPA
│   ├── repository/                  # Repositorios Spring Data
│   ├── scheduler/                   # Tareas programadas
│   ├── service/                     # Lógica de negocio (+ impl/)
│   └── validation/                  # Validaciones personalizadas
├── src/main/resources/
│   └── application.properties
└── src/test/java/                   # Pruebas unitarias
```

Los controladores, DTO, modelos, repositorios y servicios comparten la misma agrupación por módulo:

- `gestion_usuarios_acceso`
- `gestion_citas_recepcion`
- `facturacion_caja_pagos`
- `atencion_medica_enfermeria`
- `examenes_laboratorio`
- `farmacia_inventario_medicamentos`
- `agenda_medica_tareas`
- `configuracion_catalogos_sistema`

---

## Decisiones de diseño

- **Catálogos en caché:** las listas de catálogos (especialidades, sucursales, tipos y estados de cita, CIE-10 y la relación sucursal-especialidad) se guardan en la caché `catalogos` de Spring (`@Cacheable`), porque se consultan constantemente y casi no cambian. Cuando un administrador crea, edita o elimina un catálogo se limpia la caché (`@CacheEvict`) para no mostrar datos viejos. Además, los estados de cita se cargan en memoria una sola vez al arrancar (`EstadoCitaCache`), ya que la lógica de las citas depende de ellos en todo momento.
- **Base de datos relacional:** el modelo vive en PostgreSQL como tablas relacionadas entre sí con llaves foráneas (usuarios, roles, sucursales, especialidades, citas, pagos, órdenes de laboratorio, recetas, inventario, etc.). Las tablas se generan a partir de las entidades JPA.
- **Seguridad por rol:** `SecurityConfig` restringe cada ruta `/api/**` a los roles que corresponden. La sesión usa JWT sin estado.
- **Datos sensibles cifrados:** un `CryptoConverter` cifra DPI y NIT con AES de forma determinística, así se puede buscar por DPI sin descifrar toda la tabla.
- **Bloqueo de cuenta:** tras 5 intentos fallidos de inicio de sesión la cuenta se bloquea 15 minutos.
- **Pagos con patrón Strategy:** `ProcesadorPagoStrategy` con implementaciones para efectivo y tarjeta. Las operaciones de pago usan llaves de idempotencia para evitar cobros duplicados.
- **Auditoría con AOP:** la anotación `@Auditable` registra las operaciones en la bitácora sin ensuciar los servicios.
- **Tareas programadas:** cancelación automática de citas y recordatorios por correo.
- **Validación de adjuntos:** los PDF pasan por una validación de contenido y por un escaneo antivirus antes de guardarse.

---

## SOLID y OWASP

Durante todo el desarrollo **se intentó aplicar los principios SOLID y tomar como referencia las recomendaciones de OWASP**. Aun así, es posible que existan partes del proyecto donde no se aplicaron del todo o donde quedaron decisiones mejorables.

**SOLID, lo que se buscó:**

- **Responsabilidad única:** separación en capas (controlador, servicio, repositorio, DTO y modelo), cada una con un rol claro.
- **Abierto/cerrado:** el procesamiento de pagos usa el patrón Strategy, así se agrega un método de pago nuevo sin tocar los existentes.
- **Inversión de dependencias:** los controladores dependen de interfaces de servicio (`service/`) y no de sus implementaciones (`service/**/impl/`).
- **Segregación de interfaces:** los servicios se dividen por módulo en lugar de uno solo gigante.

**OWASP (Top 10), lo que se buscó:**

- **Control de acceso:** autorización por rol en cada ruta `/api/**` y autenticación con JWT.
- **Fallas criptográficas:** contraseñas con BCrypt y datos sensibles (DPI y NIT) cifrados con AES.
- **Inyección:** acceso a datos con JPA/Spring Data (consultas parametrizadas) y validación de entradas con Bean Validation.
- **Fallas de autenticación:** bloqueo temporal de la cuenta tras varios intentos fallidos.
- **Registro y monitoreo:** bitácora de operaciones mediante auditoría.
- **Archivos adjuntos:** validación del contenido del PDF y escaneo antivirus.

**Puntos conocidos por mejorar:** valores por defecto de desarrollo en `application.properties` (clave de cifrado, secreto JWT y contraseña de base de datos) que en producción se reemplazan con variables de entorno, y una política CORS abierta (`*`).

---

## Pruebas

**Pruebas manuales de la API con Postman:** antes de crear el frontend, cada endpoint se fue probando con Postman (autenticación con JWT, flujos de cada caso de uso y mensajes de error), de modo que el backend ya funcionaba de forma independiente.

**Pruebas unitarias y de controladores (JUnit y Mockito):**

```bash
mvn clean test
```

**Pruebas end to end con Cypress** (hay un spec por caso de uso, en `qa/cypress`). Con el backend corriendo en otra terminal:

```bash
cd qa/cypress
npm install
npx cypress open     # modo interactivo
npm run cy:run       # modo headless
```

> **Aviso:** es posible que algunas pruebas de Cypress no funcionen tal cual, porque al final del desarrollo se editaron cosas que ya existían cuando esas pruebas se escribieron. Esto no afecta al proyecto, que sigue funcionando correctamente.

---

## Despliegue en Azure y CI/CD

El proyecto está **publicado en Azure, junto con su base de datos PostgreSQL**, y cuenta con un pipeline de CI/CD que actualiza la imagen.

```text
git push a main
      │
      ▼
GitHub Actions (CI/CD)
      │  construye la imagen Docker
      ▼
Azure Container Registry
      │
      ▼
Azure Container Apps (se actualiza la imagen)  ──►  Base de datos PostgreSQL (Azure)
      │
      └──►  Azure Blob Storage (documentos adjuntos de las citas)
```

**Pipeline CI/CD** (`.github/workflows/deploy.yml`), que se ejecuta con cada push a la rama `main`:

1. Descarga el código del repositorio.
2. Inicia sesión en Azure Container Registry y configura Docker Buildx.
3. Construye la imagen Docker del backend y la sube al registro, etiquetada con el hash del commit y usando caché de capas para acelerar los builds.
4. Inicia sesión en Azure y actualiza la imagen de la aplicación en Azure Container Apps.
5. Borra las imágenes anteriores del registro, dejando solo la recién desplegada.

> **Nota:** el pipeline **solo actualiza la imagen**; no inicia automáticamente el sistema en Azure. El arranque se hace aparte.

Los secretos del pipeline (`REGISTRY_USERNAME`, `REGISTRY_PASSWORD`, `AZURE_CREDENTIALS`) se configuran en GitHub, y las variables de entorno de la aplicación (base de datos, correo, cifrado, JWT y Blob Storage) en el propio Container App.

---

## Herramientas de desarrollo

- **IntelliJ IDEA:** para escribir todo el código del backend.
- **Postman:** para probar la API antes de tener frontend.
- **DBeaver:** para ver visualmente la base de datos PostgreSQL (tablas, relaciones y datos).
- **GitKraken:** para los push cortos, con pocos cambios.
- **Git Bash:** para los push grandes, con muchos cambios.
- **GitHub:** repositorio y ejecución del pipeline de CI/CD.

---

## Autor

- Nombre: Allan Stiven Ramos López
