package org.umg.sistemamedicoii.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class BlobStorageService {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-documentos}")
    private String containerName;

    private BlobContainerClient containerClient;

    private synchronized BlobContainerClient getContainerClient() {
        if (containerClient == null) {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) {
                containerClient.create(); // privado por defecto, sin acceso anónimo
            }
        }
        return containerClient;
    }

    public String subir(byte[] contenido, String nombreOriginal, Integer citaId) {
        String nombreBlob = "cita-" + citaId + "/" + UUID.randomUUID() + "-" + nombreOriginal;
        BlobClient blobClient = getContainerClient().getBlobClient(nombreBlob);
        blobClient.upload(new ByteArrayInputStream(contenido), contenido.length, true);
        blobClient.setHttpHeaders(new BlobHttpHeaders().setContentType("application/pdf"));
        return blobClient.getBlobUrl();
    }
}