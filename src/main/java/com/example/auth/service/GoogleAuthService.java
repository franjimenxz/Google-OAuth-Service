package com.example.auth.service;

import com.example.auth.model.User;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.logging.Logger;

@Service
public class GoogleAuthService {

    private static final Logger logger = Logger.getLogger(GoogleAuthService.class.getName());

    @Value("${google.client.id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthService() {
        
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory())
                .setAudience(Collections.emptyList()) // Se configurará dinámicamente
                .build();
    }

    
    public User verifyGoogleToken(String idTokenString) {
        try {
            logger.info("Verificando token de Google...");
            
            
            GoogleIdTokenVerifier actualVerifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            
            GoogleIdToken idToken = actualVerifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                
                String userId = payload.getSubject();
                String email = payload.getEmail();
                Boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                logger.info("Token verificado exitosamente para el usuario: " + email);

                
                return new User(
                        userId,
                        email,
                        name,
                        pictureUrl,
                        emailVerified != null ? emailVerified : false
                );
            } else {
                logger.warning("Token de Google inválido");
                return null;
            }
        } catch (Exception e) {
            logger.severe("Error al verificar token de Google: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    
    public User authenticateUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            
            return null;
        }

        User user = verifyGoogleToken(token);
        
        if (user != null) {
            logger.info("Usuario autenticado exitosamente: " + user.getEmail());
            
        }
        
        return user;
    }
}
