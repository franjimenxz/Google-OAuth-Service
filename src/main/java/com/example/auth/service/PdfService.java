package com.example.auth.service;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfService {

    public byte[] signPdf(InputStream pdfInputStream, InputStream signatureInputStream) throws IOException {
        try (PDDocument document = PDDocument.load(pdfInputStream)) {
            PDPage page = document.getPage(0);

            byte[] signatureBytes = IOUtils.toByteArray(signatureInputStream); //lo convierte en bytes, es necesario para utilizar la libreria
            PDImageXObject signatureImage = PDImageXObject.createFromByteArray(document, signatureBytes, "firma.png");

            float x = 460;
            float y = 210;
            float width = 50;
            float height = 50;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(signatureImage, x, y, width, height); //"dibuja" la firma sin borrar lo que ya existe
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream); //guarda el pdf con la firma
            return outputStream.toByteArray(); //flujo de salida en bytes
        }
    }

}
