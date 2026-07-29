package org.umg.sistemamedicoii.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Converter
@Component
public class CryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";

    @Value("${app.security.encryption-key}")
    private String claveBase;

    @Override
    public String convertToDatabaseColumn(String textoPlano) {
        if (textoPlano == null || textoPlano.isBlank()) return textoPlano;
        try {
            SecretKeySpec clave = obtenerClave();
            byte[] iv = derivarIv(textoPlano, clave);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new IvParameterSpec(iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            byte[] resultado = new byte[iv.length + cifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(cifrado, 0, resultado, iv.length, cifrado.length);
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar el campo.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String textoCifrado) {
        if (textoCifrado == null || textoCifrado.isBlank()) return textoCifrado;
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifrado);
            byte[] iv = new byte[16];
            byte[] cifrado = new byte[datos.length - 16];
            System.arraycopy(datos, 0, iv, 0, 16);
            System.arraycopy(datos, 16, cifrado, 0, cifrado.length);

            SecretKeySpec clave = obtenerClave();
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave, new IvParameterSpec(iv));
            return new String(cipher.doFinal(cifrado), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error al descifrar el campo.", e);
        }
    }

    private SecretKeySpec obtenerClave() throws Exception {
        byte[] claveBytes = MessageDigest.getInstance("SHA-256")
                .digest(claveBase.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(claveBytes, "AES"); // 256 bits
    }

    private byte[] derivarIv(String textoPlano, SecretKeySpec clave) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(clave);
        byte[] hash = hmac.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[16];
        System.arraycopy(hash, 0, iv, 0, 16);
        return iv;
    }
}