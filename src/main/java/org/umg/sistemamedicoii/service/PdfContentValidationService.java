package org.umg.sistemamedicoii.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PdfContentValidationService {

    public void validarContenido(byte[] contenido) {
        if (contenido == null || contenido.length == 0) {
            throw new IllegalArgumentException("El archivo está vacío.");
        }

        try (PDDocument document = Loader.loadPDF(contenido)) {

            if (document.isEncrypted()) {
                throw new IllegalArgumentException(
                        "No se permiten documentos PDF protegidos con contraseña.");
            }

            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("El documento PDF no contiene páginas.");
            }

            String texto;
            try {
                texto = new PDFTextStripper().getText(document);
            } catch (IOException e) {
                texto = "";
            }
            if ((texto == null || texto.isBlank()) && !tieneContenidoVisual(document)) {
                throw new IllegalArgumentException(
                        "El documento PDF está en blanco. Suba un documento con contenido.");
            }

            if (contieneJavaScript(document)) {
                throw new IllegalArgumentException(
                        "El documento PDF contiene código JavaScript embebido, lo cual no está permitido por seguridad.");
            }

        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException e) {
            // Loader.loadPDF lanza esto cuando el PDF exige una contraseña para poder abrirlo
            throw new IllegalArgumentException(
                    "No se permiten documentos PDF protegidos con contraseña.");
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "El archivo no es un documento PDF válido o está corrupto.");
        }
    }

    /** Esta verificacion es para ver que genuinamente este
     * vacio porque puede darse el caso de que tenga una imagen */
    private boolean tieneContenidoVisual(PDDocument document) {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources == null) continue;
            for (COSName nombre : resources.getXObjectNames()) {
                try {
                    PDXObject xObject = resources.getXObject(nombre);
                    if (xObject instanceof PDImageXObject) {
                        return true;
                    }
                } catch (IOException ignored) {
                    // objeto ilegible: no lo contamos como contenido válido
                }
            }
        }
        return false;
    }

    /** recorre todo el pdf para encontrar codigo js*/
    private boolean contieneJavaScript(PDDocument document) {
        COSName js = COSName.getPDFName("JS");
        COSName javaScript = COSName.getPDFName("JavaScript");
        for (COSObject obj : document.getDocument().getObjects()) {
            COSBase base = obj.getObject();
            if (base instanceof COSDictionary dict) {
                if (dict.containsKey(js) || dict.containsKey(javaScript)) {
                    return true;
                }
            }
        }
        return false;
    }
}