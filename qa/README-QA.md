# QA de SistemaMedicoII — Cypress + SonarQube

Todo lo de esta carpeta (`qa/`) es infraestructura de pruebas *externa* al backend:
no la usa Maven ni Spring Boot para nada, así que al terminar el proyecto puedes
borrar `qa/` completa sin dejar rastro ni tener que explicar nada de esto en la
entrega. Los JUnit sí quedan en `src/test/java/...` como parte normal del proyecto
(ver sección 3).

## Estructura

```
qa/
├── sonarqube/
│   └── docker-compose.sonarqube.yml
└── cypress/
    ├── package.json
    ├── cypress.config.js
    ├── support/
    │   └── e2e.js
    └── e2e/
        ├── 00_verificar_dpi.cy.js
        ├── 01_login.cy.js
        └── 02_registro_paciente.cy.js
```

## 1. Cypress

Requiere Node.js instalado.

```bash
cd qa/cypress
npm install
```

En **otra terminal**, levanta el backend real (las pruebas visitan las páginas
estáticas que sirve Spring Boot: `index.html`, `login.html`, `registro.html`):

```bash
mvn spring-boot:run
```

Y de vuelta en `qa/cypress`:

```bash
npx cypress open      # modo interactivo
# o
npm run cy:run        # modo headless, para CI
```

Los specs usan `cy.intercept` para simular las respuestas de `/api/portal/...`,
así que no dependen de datos reales en tu base de datos.

## 2. SonarQube local con Docker

```bash
docker compose -f qa/sonarqube/docker-compose.sonarqube.yml up -d
```

1. Abre `http://localhost:9000` (usuario/clave inicial: `admin` / `admin`, te pedirá cambiarla).
2. Crea el proyecto con la key `sistemamedicoii` (o deja que el primer análisis lo cree).
3. Genera un token: **My Account → Security → Generate Token**.
4. Corre el análisis desde la raíz del proyecto (esto sí toca el `pom.xml`, ejecuta
   los JUnit, genera cobertura y la sube a Sonar):

```bash
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar -Dsonar.token=TU_TOKEN
```

> El plugin de Sonar no está agregado al `<build>` del `pom.xml` a propósito, para
> que no intente conectarse a un servidor en cada `mvn install` normal.

## 3. Lo que SÍ se queda en tu proyecto (no borrar)

- `pom.xml` (raíz): tiene JaCoCo + las dependencias de test agregadas.
- `src/test/java/org/umg/sistemamedicoii/service/UsuarioServiceImplTest.java`
- `src/test/java/org/umg/sistemamedicoii/controller/PortalControllerTest.java`

Correr solo esos con cobertura HTML:

```bash
mvn clean test
# reporte en target/site/jacoco/index.html
```

## 4. Cobertura actual: CU-00 y CU-02

Login, bloqueo por intentos (RN-CU00-02/03), verificación de DPI, y registro de
paciente externo. Falta CU-01, CU-03 a CU-16. Siguiente lote pendiente de definir.
