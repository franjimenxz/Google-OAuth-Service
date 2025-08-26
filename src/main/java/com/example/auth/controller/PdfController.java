package com.example.auth.controller;

import org.slf4j.Logger;
import com.example.auth.service.GoogleAuthService;
import com.example.auth.service.PdfService;
import com.google.api.services.drive.model.File;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class PdfController {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(PdfController.class);

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private PdfService pdfService;

    @PostMapping("/sign-from-drive/{userEmail}")
    public ResponseEntity<Map<String, String>> signPdfFromDrive(
            @PathVariable String userEmail,
            @RequestParam("pdfFileId") String pdfFileId, //Id de PDF
            @RequestParam("signatureFileId") String signatureFileId, //Id de firma
            @RequestParam(value = "parentFolderId", required = false) String parentFolderId) { //Id de carpeta donde quiero guardar el recibo firmado

        if (signatureFileId == null || signatureFileId.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Falta el ID del archivo de la firma."));
        }

        String accessToken = googleAuthService.getAccessToken(userEmail);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Usuario no autenticado."));
        }

        logger.info("Iniciando proceso de firma para el archivo de Drive: " + pdfFileId);

        // Paso 1: Descargar el PDF del recibo Y la firma de Google Drive
        try (InputStream pdfStream = googleAuthService.downloadDriveFileAsInputStream(accessToken, pdfFileId);
             InputStream signatureStream = googleAuthService.downloadDriveFileAsInputStream(accessToken, signatureFileId)) { // Cambiado para descargar la firma

            if (pdfStream == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Archivo PDF no encontrado en Google Drive."));
            }
            if (signatureStream == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("message", "Archivo de firma no encontrado en Google Drive."));
            }
            //Obtengo el nombre para luego renombrar el recibo firmado
            String originalFileName = googleAuthService.getFileNameFromId(accessToken,pdfFileId);

            // Paso 2: Llamar al PdfService para firmar el documento
            byte[] signedPdfBytes = pdfService.signPdf(pdfStream, signatureStream);

            // Paso 3: Subir el PDF firmado de nuevo a Google Drive
            String newFileName = originalFileName.toUpperCase().replace("EMPLEADOR.PDF","FIRMADO.PDF");

            File uploadedFile = googleAuthService.uploadSignedPdfToDrive(accessToken, newFileName, signedPdfBytes, parentFolderId);

            Map<String, String> response = new HashMap<>(); //Para verificar que anda
            response.put("message", "PDF firmado y subido a Google Drive exitosamente.");
            response.put("fileId", uploadedFile.getId());
            response.put("fileName", uploadedFile.getName());

            logger.info("PDF firmado y subido a Drive con éxito. ID: " + uploadedFile.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error al procesar la firma del PDF: ",e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Error interno del servidor: " + e.getMessage()));
        }
    }
}
