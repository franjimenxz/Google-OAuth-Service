package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthRequest;
import com.example.auth.model.User;
import com.example.auth.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private static final Logger logger = Logger.getLogger(AuthController.class.getName());

    @Autowired
    private GoogleAuthService googleAuthService;

    
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            logger.info("Recibida solicitud de autenticación con Google");
            
            
            User user = googleAuthService.authenticateUser(request.getToken());
            
            if (user != null) {
                
                AuthResponse response = new AuthResponse(
                    true, 
                    "Autenticación exitosa", 
                    user
                );
                
                logger.info("Autenticación exitosa para usuario: " + user.getEmail());
                return ResponseEntity.ok(response);
                
            } else {
                
                AuthResponse response = new AuthResponse(
                    false, 
                    "Token de Google inválido"
                );
                
                logger.warning("Intento de autenticación fallido - Token inválido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            logger.severe("Error en autenticación: " + e.getMessage());
            e.printStackTrace();
            
            AuthResponse response = new AuthResponse(
                false, 
                "Error interno del servidor durante la autenticación"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("API de autenticación funcionando correctamente");
    }

    
    @GetMapping("/info")
    public ResponseEntity<Object> getApiInfo() {
        return ResponseEntity.ok(new Object() {
            public final String version = "1.0.0";
            public final String description = "API de autenticación con Google";
            public final String[] endpoints = {
                "POST /api/auth/google - Autenticar con token de Google",
                "GET /api/auth/status - Verificar estado de la API",
                "GET /api/auth/info - Información de la API"
            };
            public final String usage = "Envía un POST a /api/auth/google con un JSON: {'token': 'tu_google_token'}";
        });
    }
}
