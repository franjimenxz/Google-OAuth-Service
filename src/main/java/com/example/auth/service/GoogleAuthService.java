package com.example.auth.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.auth.model.User;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class GoogleAuthService {

    private static final Logger logger = Logger.getLogger(GoogleAuthService.class.getName());

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    private final GoogleIdTokenVerifier verifier;

    // In-memory storage for access tokens (email -> accessToken)
    private final Map<String, String> accessTokenStore = new ConcurrentHashMap<>();
    // In-memory storage for refresh tokens (email -> refreshToken)
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

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

    
    public TokenResponse exchangeCodeForTokens(String code, String redirectUri) throws IOException {
        return new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                googleClientId,
                googleClientSecret,
                code,
                redirectUri)
                .execute();
    }

    public User authenticateUser(String idTokenString, String accessToken, String refreshToken) {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            logger.warning("ID Token vacío o nulo recibido");
            return null;
        }

        User user = verifyGoogleToken(idTokenString);

        if (user != null) {
            logger.info("Usuario autenticado exitosamente: " + user.getEmail());
            // Store the access token and refresh token
            accessTokenStore.put(user.getEmail(), accessToken);
            if (refreshToken != null && !refreshToken.trim().isEmpty()) {
                refreshTokenStore.put(user.getEmail(), refreshToken);
            }
        }

        return user;
    }

    public String getAccessToken(String userEmail) {
        return accessTokenStore.get(userEmail);
    }

    public String getRefreshToken(String userEmail) {
        return refreshTokenStore.get(userEmail);
    }

    public List<String> listCalendarEvents(String accessTokenString) {
        List<String> eventSummaries = new ArrayList<>();
        try {
            logger.info("Iniciando búsqueda de eventos del calendario con Access Token");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Calendar service
            com.google.api.services.calendar.Calendar service = 
                new com.google.api.services.calendar.Calendar.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // List events from primary calendar
            com.google.api.services.calendar.model.Events events = service.events()
                .list("primary")
                .setMaxResults(10)
                .setTimeMin(new com.google.api.client.util.DateTime(System.currentTimeMillis()))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

            if (events.getItems() != null && !events.getItems().isEmpty()) {
                for (com.google.api.services.calendar.model.Event event : events.getItems()) {
                    String summary = event.getSummary();
                    if (summary == null || summary.isEmpty()) {
                        summary = "Sin título";
                    }
                    
                    String startTime = "";
                    if (event.getStart() != null) {
                        if (event.getStart().getDateTime() != null) {
                            startTime = " - " + event.getStart().getDateTime().toString();
                        } else if (event.getStart().getDate() != null) {
                            startTime = " - " + event.getStart().getDate().toString();
                        }
                    }
                    
                    eventSummaries.add(summary + startTime);
                }
                logger.info("Encontrados " + eventSummaries.size() + " eventos del calendario");
            } else {
                logger.info("No se encontraron eventos próximos en el calendario");
                eventSummaries.add("No hay eventos próximos");
            }

        } catch (Exception e) {
            logger.severe("Error al listar eventos del calendario: " + e.getMessage());
            e.printStackTrace();
            eventSummaries.add("Error al obtener eventos: " + e.getMessage());
        }
        return eventSummaries;
    }
}

