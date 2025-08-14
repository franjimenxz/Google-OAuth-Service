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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.CalendarEventDto;
import com.example.auth.dto.DriveFileDto;
import com.example.auth.dto.GoogleAuthRequest;
import com.example.auth.dto.TaskDto;
import com.example.auth.model.User;
import com.example.auth.service.GoogleAuthService;
import com.google.api.client.auth.oauth2.TokenResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")

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
            logger.info("Callback recibido - Procesando autenticación");
            
            // Procesar autenticación
            String redirectUri = "http://localhost:8080/api/auth/google/callback";
            logger.info("Usando redirect_uri: " + redirectUri);
            TokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(code, redirectUri);
            
            String idTokenString = tokenResponse.get("id_token").toString();
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            
            User user = googleAuthService.authenticateUser(idTokenString, accessToken, refreshToken);
            
            if (user != null) {
                logger.info("Autenticación exitosa para usuario: " + user.getEmail());
                
                // Pasar datos del usuario en la URL
                String userParams = "?auth=success" +
                    "&user_id=" + java.net.URLEncoder.encode(user.getId(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_email=" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_name=" + java.net.URLEncoder.encode(user.getName(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&user_picture=" + java.net.URLEncoder.encode(user.getPicture(), java.nio.charset.StandardCharsets.UTF_8) +
                    "&email_verified=" + user.isEmailVerified();
                
                logger.info("Redirigiendo con datos del usuario en URL");
                
                // Redirección al frontend con datos del usuario
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "https://test-api-google.netlify.app/home" + userParams)
                    .build();
                
            } else {
                logger.warning("Fallo en la verificación del ID Token");
                
                // Redirección al frontend con error
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "https://test-api-google.netlify.app/home?auth=error&message=token_verification_failed")
                    .build();
            }
            
            
        } catch (Exception e) {
            logger.severe("Error en callback: " + e.getMessage());
            e.printStackTrace();
            
            String errorType = "server_error";
            
            // Diagnóstico de errores
            if (e.getMessage().contains("401")) {
                errorType = "client_secret_error";
                logger.severe("Posible problema: Client Secret incorrecto o faltante");
            } else if (e.getMessage().contains("redirect_uri")) {
                errorType = "redirect_uri_error";
                logger.severe("Posible problema: redirect_uri no autorizado en Google Console");
            }
            
            // Redirección al frontend con error
            return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "https://test-api-google.netlify.app/home?auth=error&type=" + errorType)
                .build();
        }
    }

    @GetMapping("/calendar/events/{userEmail}")
    public ResponseEntity<Object> getCalendarEvents(@PathVariable String userEmail) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            if (accessToken == null) {
                logger.warning("Access Token no encontrado para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Object() {
                            public final boolean success = false;
                            public final String message = "No se encontró Access Token para el usuario. Por favor, autentícate primero.";
                            public final String user = userEmail;
                        });
            }

            logger.info("Obteniendo eventos del calendario para: " + userEmail);
            List<CalendarEventDto> events = googleAuthService.listCalendarEvents(accessToken);

            final String email = userEmail;
            final int count = events.size();

            Object response = new Object() {
                public final boolean success = true;
                public final String message = "Eventos del calendario obtenidos exitosamente";
                public final String userEmail = email;
                public final int eventCount = count;
                public final List<CalendarEventDto> eventsList = events;
                public final long timestamp = System.currentTimeMillis();
                public final String note = "Mostrando próximos " + count + " eventos";
            };

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.severe("Error al obtener eventos del calendario: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Error interno del servidor: " + e.getMessage();
                    });
        }
    }

    @GetMapping("/drive/files/{userEmail}")
    public ResponseEntity<Object> getDriveFiles(@PathVariable String userEmail) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            if (accessToken == null) {
                logger.warning("Access Token no encontrado para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Object() {
                            public final boolean success = false;
                            public final String message = "No se encontró Access Token para el usuario. Por favor, autentícate primero.";
                            public final String user = userEmail;
                        });
            }

            logger.info("Obteniendo archivos de Google Drive para: " + userEmail);
            List<DriveFileDto> files = googleAuthService.listDriveFiles(accessToken);

            final String email = userEmail;
            final int count = files.size();

            Object response = new Object() {
                public final boolean success = true;
                public final String message = "Archivos de Google Drive obtenidos exitosamente";
                public final String userEmail = email;
                public final int fileCount = count;
                public final List<DriveFileDto> filesList = files;
                public final long timestamp = System.currentTimeMillis();
                public final String note = "Mostrando últimos " + count + " archivos";
            };

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.severe("Error al obtener archivos de Google Drive: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Error interno del servidor: " + e.getMessage();
                    });
        }
    }

    @PostMapping("/drive/upload/{userEmail}")
    public ResponseEntity<Object> uploadFileToDrive(@PathVariable String userEmail,
                                                   @RequestParam("file") MultipartFile file) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            if (accessToken == null) {
                logger.warning("Access Token no encontrado para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Object() {
                            public final boolean success = false;
                            public final String message = "No se encontró Access Token para el usuario. Por favor, autentícate primero.";
                            public final String user = userEmail;
                        });
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new Object() {
                            public final boolean success = false;
                            public final String message = "No se ha seleccionado ningún archivo";
                        });
            }

            logger.info("Subiendo archivo a Google Drive para: " + userEmail);
            
            String fileName = file.getOriginalFilename();
            String mimeType = file.getContentType();
            byte[] fileContent = file.getBytes();
            
            List<String> result = googleAuthService.uploadFileToDrive(accessToken, fileName, mimeType, fileContent);

            final String email = userEmail;
            final List<String> uploadResult = result;

            Object response = new Object() {
                public final boolean success = true;
                public final String message = "Archivo subido exitosamente";
                public final String userEmail = email;
                public final String fileName = file.getOriginalFilename();
                public final String fileSize = file.getSize() + " bytes";
                public final List<String> result = uploadResult;
                public final long timestamp = System.currentTimeMillis();
            };

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.severe("Error al subir archivo a Google Drive: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Object() {
                        public final boolean success = false;
                        public final String message = "Error interno del servidor: " + e.getMessage();
                    });
        }
    }

    @GetMapping("/tasks/{userEmail}")
    public ResponseEntity<Object> getTasks(@PathVariable String userEmail) {
        try {
            String accessToken = googleAuthService.getAccessToken(userEmail);
            if (accessToken == null) {
                logger.warning("Access Token no encontrado para el usuario: " + userEmail);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Object() {
                            public final boolean success = false;
                            public final String message = "No se encontró Access Token para el usuario. Por favor, autentícate primero.";
                            public final String user = userEmail;
                        });
            }

            logger.info("Obteniendo tareas de Google Tasks para: " + userEmail);
            List<TaskDto> tasks = googleAuthService.listTasks(accessToken);

            final String email = userEmail;
            final int count = tasks.size();

            Object response = new Object() {
                public final boolean success = true;
                public final String message = "Tareas obtenidas exitosamente";
                public final String userEmail = email;
                public final int taskCount = count;
                public final List<TaskDto> tasksList = tasks;
                public final long timestamp = System.currentTimeMillis();
                public final String note = "Mostrando tareas de todas las listas";
            };

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            logger.severe("Error al obtener tareas: " + e.getMessage());
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
            public final String version = "2.2.0";
            public final String description = "API de autenticación con Google y servicios integrados (Calendar, Drive, Tasks)";
            public final String[] endpoints = {
                "POST /api/auth/google - Autenticar con código de autorización",
                "GET /api/auth/google/callback - Callback de Google OAuth",
                "GET /api/auth/status - Verificar estado de la API",
                "GET /api/auth/info - Información de la API",
                "GET /api/auth/calendar/events/{userEmail} - Listar eventos del calendario",
                "GET /api/auth/drive/files/{userEmail} - Listar archivos de Google Drive",
                "POST /api/auth/drive/upload/{userEmail} - Subir archivo a Google Drive",
                "GET /api/auth/tasks/{userEmail} - Listar tareas de Google Tasks"
            };
            public final String usage = "Envía un POST a /api/auth/google con un JSON: {'code': 'authorization_code'}";
            public final String note = "Endpoints de consulta (GET) y upload de archivos (POST). Todos requieren autenticación previa";
        });
    }
}

