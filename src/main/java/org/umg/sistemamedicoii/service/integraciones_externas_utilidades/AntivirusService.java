package org.umg.sistemamedicoii.service.integraciones_externas_utilidades;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.exception.AntivirusUnavailableException;
import org.umg.sistemamedicoii.exception.VirusDetectedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class AntivirusService {

    private static final Logger logger = LoggerFactory.getLogger(AntivirusService.class);

    // Solo permite UN escaneo de clamscan a la vez.
    // Esto evita que N subidas simultáneas disparen N copias de la base de
    // firmas (~1-1.5GB cada una) en RAM al mismo tiempo y tumben el container.
    // Las demás peticiones simplemente esperan su turno en la cola.
    private static final Semaphore SCAN_LOCK = new Semaphore(1, true);

    // Tiempo máximo que se le da a clamscan para cargar firmas + escanear.
    // Si se pasa, se asume que el proceso está trabado y se mata.
    private static final long SCAN_TIMEOUT_SECONDS = 30;

    // Tiempo máximo esperando a que se libere el semáforo (otro escaneo en curso).
    private static final long QUEUE_WAIT_SECONDS = 60;

    public void escanear(byte[] contenido) {
        boolean permisoAdquirido = false;
        try {
            permisoAdquirido = SCAN_LOCK.tryAcquire(QUEUE_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!permisoAdquirido) {
                logger.error("Timeout esperando turno para escanear (cola de antivirus saturada).");
                throw new AntivirusUnavailableException(
                        "El sistema está procesando muchos archivos en este momento. Intenta de nuevo en unos segundos.");
            }

            ejecutarEscaneo(contenido);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AntivirusUnavailableException("El escaneo fue interrumpido.", e);
        } finally {
            if (permisoAdquirido) {
                SCAN_LOCK.release();
            }
        }
    }

    private void ejecutarEscaneo(byte[] contenido) {
        Path tempFile = null;
        Process process = null;
        try {
            // 1. Crear un archivo temporal en el mismo contenedor
            tempFile = Files.createTempFile("scan_doc_", ".pdf");
            Files.write(tempFile, contenido);

            // 2. Ejecutar el comando clamscan nativo de Linux
            ProcessBuilder processBuilder = new ProcessBuilder("clamscan", "--no-summary", tempFile.toString());
            processBuilder.redirectErrorStream(true); // junta stderr con stdout para no bloquear el pipe
            process = processBuilder.start();

            // 3. Leer el output MIENTRAS el proceso corre (evita que se cuelgue
            //    si el buffer del pipe se llena antes del waitFor)
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 4. Esperar a que termine, con timeout explícito
            boolean terminoATiempo = process.waitFor(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!terminoATiempo) {
                process.destroyForcibly();
                logger.error("clamscan no respondió en {}s, proceso terminado a la fuerza.", SCAN_TIMEOUT_SECONDS);
                throw new AntivirusUnavailableException("El motor de antivirus tardó demasiado en responder.");
            }

            int exitCode = process.exitValue();

            // Código 0: Archivo limpio
            // Código 1: Virus detectado
            // Código 2 o mayor: Error de ClamAV
            if (exitCode == 1) {
                logger.warn("Antivirus detectó una amenaza: {}", output);
                throw new VirusDetectedException("Se ha detectado contenido malicioso en el archivo.");
            } else if (exitCode != 0) {
                logger.error("Error al ejecutar clamscan. Código de salida: {}. Output: {}", exitCode, output);
                throw new AntivirusUnavailableException("El motor de antivirus interno falló al escanear.");
            }

            logger.info("Archivo escaneado localmente. Estado: LIMPIO.");

        } catch (IOException e) {
            logger.error("Excepción de I/O al ejecutar el antivirus local: ", e);
            throw new AntivirusUnavailableException("No se pudo ejecutar el escaneo de seguridad local.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new AntivirusUnavailableException("El escaneo fue interrumpido.", e);
        } finally {
            // 5. Limpiar siempre el archivo temporal para no llenar el disco
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }
}