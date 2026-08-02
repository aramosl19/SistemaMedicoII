package org.umg.sistemamedicoii.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.umg.sistemamedicoii.exception.AntivirusUnavailableException;
import org.umg.sistemamedicoii.exception.VirusDetectedException;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class AntivirusService {

    @Value("${clamav.host}")
    private String host;

    @Value("${clamav.port}")
    private int port;

    @Value("${clamav.timeout-ms}")
    private int timeoutMs;

    private static final int CHUNK_SIZE = 8192;

    public void escanear(byte[] contenido) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes("zINSTREAM\0");

            int offset = 0;
            while (offset < contenido.length) {
                int len = Math.min(CHUNK_SIZE, contenido.length - offset);
                out.writeInt(len);
                out.write(contenido, offset, len);
                offset += len;
            }
            out.writeInt(0); // chunk final vacío indica fin de stream
            out.flush();

            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[512];
            int read = in.read(buffer);
            String respuesta = read > 0 ? new String(buffer, 0, read, StandardCharsets.UTF_8) : "";

            if (respuesta.contains("FOUND")) {
                throw new VirusDetectedException(respuesta.trim());
            }
            if (!respuesta.contains("OK")) {
                throw new AntivirusUnavailableException("Respuesta inesperada del motor antivirus: " + respuesta);
            }
        } catch (IOException e) {
            throw new AntivirusUnavailableException("No se pudo contactar al servicio de antivirus.", e);
        }
    }
}