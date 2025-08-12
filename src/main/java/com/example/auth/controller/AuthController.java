package com.example.auth.controller;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.GoogleAuthRequest;
import com.example.auth.model.User;
import com.example.auth.service.GoogleAuthService;
import com.google.api.client.auth.oauth2.TokenResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private static final Logger logger = Logger.getLogger(AuthController.class.getName());

    @Autowired
    private GoogleAuthService googleAuthService;

    
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        try {
            logger.info("Recibida solicitud de autenticación con Google (código de autorización)");

            // Define the redirect URI used in the frontend for the authorization code flow
            // This MUST match the Authorized Redirect URI configured in Google Cloud Console
            String redirectUri = "http://localhost:8080/api/auth/google/callback";

            // Exchange authorization code for tokens
            TokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(request.getCode(), redirectUri);

            String idTokenString = tokenResponse.get("id_token").toString();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();

            User user = googleAuthService.authenticateUser(idTokenString, accessToken, refreshToken);

            if (user != null) {
                AuthResponse response = new AuthResponse(
                    true,
                    "Autenticación exitosa",
                    user // Return user data to frontend
                );

                logger.info("Autenticación exitosa para usuario: " + user.getEmail());
                return ResponseEntity.ok(response);

            } else {
                AuthResponse response = new AuthResponse(
                    false,
                    "Código de autorización inválido o fallo en la verificación del ID Token"
                );

                logger.warning("Intento de autenticación fallido - Código inválido o ID Token no verificado");
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

    //  Endpoint para procesar autenticación usando cookies HTTPOnly
    @PostMapping("/google/cookie")
    public ResponseEntity<AuthResponse> authenticateWithCookie(HttpServletRequest request, 
                                                             HttpServletResponse response) {
        try {
            logger.info(" Procesando autenticación con cookie HTTPOnly");
            
            // Buscar cookies con el código de autorización y state
            String authCode = null;
            String oauthState = null;
            
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("google_auth_code".equals(cookie.getName())) {
                        authCode = cookie.getValue();
                    } else if ("oauth_state".equals(cookie.getName())) {
                        oauthState = cookie.getValue();
                    }
                }
            }
            
            if (authCode == null) {
                logger.warning(" No se encontró cookie de código de autorización");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "No se encontró código de autorización en cookie"));
            }
            
            logger.info(" Código de autorización encontrado en cookie");
            
            // Usar el endpoint existente para procesar el código
            String redirectUri = "http://localhost:8080/api/auth/google/callback";
            TokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(authCode, redirectUri);
            
            String idTokenString = tokenResponse.get("id_token").toString();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            
            User user = googleAuthService.authenticateUser(idTokenString, accessToken, refreshToken);
            
            if (user != null) {
                // Limpiar cookies de autenticación (ya no las necesitamos)
                Cookie clearAuthCode = new Cookie("google_auth_code", "");
                clearAuthCode.setMaxAge(0);
                clearAuthCode.setPath("/");
                clearAuthCode.setHttpOnly(true);
                response.addCookie(clearAuthCode);
                
                if (oauthState != null) {
                    Cookie clearState = new Cookie("oauth_state", "");
                    clearState.setMaxAge(0);
                    clearState.setPath("/");
                    clearState.setHttpOnly(true);
                    response.addCookie(clearState);
                }
                
                logger.info(" Autenticación exitosa para usuario: " + user.getEmail());
                logger.info(" Cookies de autenticación limpiadas");
                
                AuthResponse authResponse = new AuthResponse(
                    true,
                    "Autenticación exitosa con cookie HTTPOnly - Máxima seguridad",
                    user
                );
                
                return ResponseEntity.ok(authResponse);
                
            } else {
                logger.warning(" Fallo en la verificación del ID Token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Error al verificar el ID Token"));
            }
            
        } catch (Exception e) {
            logger.severe(" Error en autenticación con cookie: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponse(false, "Error interno del servidor: " + e.getMessage()));
        }
    }

    
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("API de autenticación funcionando correctamente");
    }

    
    @GetMapping("/google/callback")
    public ResponseEntity<String> handleGoogleCallback(@RequestParam("code") String code, 
                                                      @RequestParam(value = "error", required = false) String error,
                                                      @RequestParam(value = "state", required = false) String state,
                                                      HttpServletResponse response) {
        if (error != null) {
            logger.warning("Error en callback de Google: " + error);
            return ResponseEntity.badRequest().body("Error de autenticación: " + error);
        }

        try {
            logger.info(" Callback recibido - Procesando directamente con máxima seguridad");
            
            // OPCIÓN A: Procesar TODO aquí (sin endpoint adicional)
            String redirectUri = "http://localhost:8080/api/auth/google/callback";
            logger.info(" Usando redirect_uri: " + redirectUri);
            TokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(code, redirectUri);
            
            String idTokenString = tokenResponse.get("id_token").toString();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            
            User user = googleAuthService.authenticateUser(idTokenString, accessToken, refreshToken);
            
            if (user != null) {
                logger.info("Autenticación exitosa para usuario: " + user.getEmail());
                
                // OPCIÓN SIMPLE: Pasar datos del usuario en la URL
                String userParams = "?auth=success" +
                    "&user_id=" + java.net.URLEncoder.encode(user.getId(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_email=" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_name=" + java.net.URLEncoder.encode(user.getName(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_picture=" + java.net.URLEncoder.encode(user.getPicture(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&email_verified=" + user.isEmailVerified();
                
                logger.info(" Redirigiendo con datos del usuario en URL");
                
                // Redirección directa con todos los datos del usuario
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/home" + userParams)
                    .build();
                
            } else {
                logger.warning(" Fallo en la verificación del ID Token");
                
                // Redirección directa al frontend con error
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/home?auth=error&message=token_verification_failed")
                    .build();
            }
            
            /* OPCIÓN B: Almacenar en cookies para endpoint separado
            Cookie authCodeCookie = new Cookie("google_auth_code", code);
            authCodeCookie.setHttpOnly(true);
            authCodeCookie.setSecure(false);
            authCodeCookie.setPath("/");
            authCodeCookie.setMaxAge(600);
            authCodeCookie.setAttribute("SameSite", "Lax");
            response.addCookie(authCodeCookie);
            
            String html = "...";  // HTML para notificar sobre cookies
            return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
            */
            
            /* OPCIÓN 2: Procesar tokens aquí y redirigir con datos del usuario
            String redirectUri = "http://localhost:8080/api/auth/google/callback";
            TokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(code, redirectUri);
            
            String idTokenString = tokenResponse.get("id_token").toString();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            
            User user = googleAuthService.authenticateUser(idTokenString, accessToken, refreshToken);
            
            if (user != null) {
                // Crear un token temporal o JWT para enviar al frontend
                String userDataJson = URLEncoder.encode(
                    "{\"id\":\"" + user.getId() + "\"," +
                    "\"email\":\"" + user.getEmail() + "\"," +
                    "\"name\":\"" + user.getName() + "\"," +
                    "\"picture\":\"" + user.getPicture() + "\"," +
                    "\"emailVerified\":" + user.isEmailVerified() + "}", 
                    StandardCharsets.UTF_8
                );
                
                String redirectUrl = "http://localhost:4200/home?success=true&user=" + userDataJson;
                
                String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head><title>Autenticación exitosa</title></head>\n" +
                    "<body>\n" +
                    "<h1>Autenticación exitosa</h1>\n" +
                    "<p>Redirigiendo...</p>\n" +
                    "<script>\n" +
                    "if (window.opener) {\n" +
                    "  window.opener.postMessage({success: true, user: " + 
                        "{id: '" + user.getId() + "', email: '" + user.getEmail() + "', " +
                        "name: '" + user.getName() + "', picture: '" + user.getPicture() + "', " +
                        "emailVerified: " + user.isEmailVerified() + "}}, '*');\n" +
                    "  window.close();\n" +
                    "} else {\n" +
                    "  window.location.href = '" + redirectUrl + "';\n" +
                    "}\n" +
                    "</script>\n" +
                    "</body>\n" +
                    "</html>";
                
                return ResponseEntity.ok().header("Content-Type", "text/html").body(html);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Error al procesar la autenticación");
            }
            */
            
        } catch (Exception e) {
            logger.severe(" Error en callback: " + e.getMessage());
            e.printStackTrace();
            
            String errorType = "server_error";
            
            // Mejorar diagnóstico de errores
            if (e.getMessage().contains("401")) {
                errorType = "client_secret_error";
                logger.severe(" Posible problema: Client Secret incorrecto o faltante");
            } else if (e.getMessage().contains("redirect_uri")) {
                errorType = "redirect_uri_error";
                logger.severe(" Posible problema: redirect_uri no autorizado en Google Console");
            }
            
            // Redirección directa al frontend con error
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "http://localhost:4200/home?auth=error&type=" + errorType)
                .build();
        }
    }

    //  Endpoint para obtener eventos del calendario
    @CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
    @GetMapping("/calendar/events/{userEmail}")
    public ResponseEntity<Object> getCalendarEvents(@PathVariable String userEmail) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            if (accessToken == null) {
                logger.warning(" Access Token no encontrado para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "No se encontró Access Token para el usuario. Por favor, autentícate primero.";
                        public final String user = userEmail;
                    });
            }

            logger.info("📅 Obteniendo eventos del calendario para: " + userEmail);
            List<String> events = googleAuthService.listCalendarEvents(accessToken);
            
            final String email = userEmail;
            final List<String> calendarEvents = events;
            final int count = events.size();
            
            Object response = new Object() {
                public final boolean success = true;
                public final String message = "Eventos del calendario obtenidos exitosamente";
                public final String userEmail = email;
                public final int eventCount = count;
                public final List<String> events = calendarEvents;
                public final long timestamp = System.currentTimeMillis();
                public final String note = "Mostrando próximos " + count + " eventos";
            };
            
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                .header("Access-Control-Allow-Headers", "*")
                .body(response);
            
        } catch (Exception e) {
            logger.severe("❌ Error al obtener eventos del calendario: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Object() {
                    public final boolean success = false;
                    public final String message = "Error interno del servidor: " + e.getMessage();
                });
        }
    }

    //  Endpoint para verificar tokens almacenados
    @GetMapping("/tokens/{userEmail}")
    public ResponseEntity<Object> getUserTokens(@PathVariable String userEmail) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            String refreshToken = googleAuthService.getRefreshToken(userEmail);
            
            if (accessToken == null) {
                logger.warning(" Tokens no encontrados para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "No se encontraron tokens para el usuario: " + userEmail;
                    });
            }
            
            // Devolver información de tokens (sin exponer tokens completos)
            final String email = userEmail;
            final String accessPreview = accessToken.substring(0, Math.min(20, accessToken.length())) + "...";
            final boolean hasRefresh = refreshToken != null;
            final String refreshPreview = refreshToken != null ? 
                refreshToken.substring(0, Math.min(20, refreshToken.length())) + "..." : null;
            
            Object tokenResponse = new Object() {
                public final boolean success = true;
                public final String message = "Tokens encontrados y disponibles";
                public final String userEmail = email;
                public final String accessTokenPreview = accessPreview;
                public final boolean hasRefreshToken = hasRefresh;
                public final String refreshTokenPreview = refreshPreview;
                public final long timestamp = System.currentTimeMillis();
            };
            
            logger.info(" Información de tokens devuelta para usuario: " + userEmail);
            return ResponseEntity.ok(tokenResponse);
            
        } catch (Exception e) {
            logger.severe("Error al obtener tokens del usuario: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Object() {
                    public final boolean success = false;
                    public final String message = "Error interno del servidor: " + e.getMessage();
                });
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Object> getApiInfo() {
        return ResponseEntity.ok(new Object() {
            public final String version = "1.0.0";
            public final String description = "API de autenticación con Google";
            public final String[] endpoints = {
                "POST /api/auth/google - Autenticar con código de autorización",
                "POST /api/auth/google/cookie - Autenticar usando cookie HTTPOnly (más seguro)",
                "GET /api/auth/google/callback - Callback de Google OAuth",
                "GET /api/auth/status - Verificar estado de la API",
                "GET /api/auth/info - Información de la API",
                "GET /api/auth/calendar/events/{userEmail} - Listar eventos del calendario para un usuario (requiere Access Token almacenado)"
            };
            public final String usage = "Envía un POST a /api/auth/google con un JSON: {'code': 'authorization_code'}";
        });
    }
}

