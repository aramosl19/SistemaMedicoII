const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8080',
    supportFile: 'support/e2e.js',
    specPattern: 'e2e/**/*.cy.js',
    video: false,
    setupNodeEvents(on, config) {
      // FIX: Chrome (no Electron) reutiliza su caché de disco entre
      // corridas de Cypress cuando se revisita la MISMA URL (ej. siempre
      // '/registro.html?dpi=1234567890123' en el beforeEach), sirviendo
      // una copia vieja de js/app.js sin volver a pedirla al servidor.
      // Esto desactiva la caché HTTP del navegador que lanza Cypress
      // para que cada visita sea siempre una carga 100% fresca.
      on('before:browser:launch', (browser = {}, launchOptions) => {
        if (browser.family === 'chromium' && browser.name !== 'electron') {
          launchOptions.args.push('--disable-application-cache');
          launchOptions.args.push('--disk-cache-size=0');
          launchOptions.args.push('--media-cache-size=0');
        }
        return launchOptions;
      });
      return config;
    },
  },
});