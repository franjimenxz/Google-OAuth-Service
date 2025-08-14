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

import com.example.auth.dto.CalendarEventDto;
import com.example.auth.dto.DriveFileDto;
import com.example.auth.dto.TaskDto;
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

    public List<CalendarEventDto> listCalendarEvents(String accessTokenString) {
        List<CalendarEventDto> eventList = new ArrayList<>();
        try {
            logger.info("Starting calendar events search with Access Token");

            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            com.google.api.services.calendar.Calendar service =
                    new com.google.api.services.calendar.Calendar.Builder(
                            new NetHttpTransport(),
                            GsonFactory.getDefaultInstance(),
                            new HttpCredentialsAdapter(credentials))
                            .setApplicationName("Google Auth API")
                            .build();

            com.google.api.services.calendar.model.Events events = service.events()
                    .list("primary")
                    .setMaxResults(10)
                    .setTimeMin(new com.google.api.client.util.DateTime(System.currentTimeMillis()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            if (events.getItems() != null && !events.getItems().isEmpty()) {
                for (com.google.api.services.calendar.model.Event event : events.getItems()) {
                    String title = event.getSummary() != null ? event.getSummary() : "No title";
                    String description = event.getDescription() != null ? event.getDescription() : "";
                    String dateTime = "";

                    if (event.getStart() != null) {
                        if (event.getStart().getDateTime() != null) {
                            dateTime = event.getStart().getDateTime().toString();
                        } else if (event.getStart().getDate() != null) {
                            dateTime = event.getStart().getDate().toString();
                        }
                    }

                    eventList.add(new CalendarEventDto(title, description, dateTime));
                }
                logger.info("Found " + eventList.size() + " calendar events");
            } else {
                logger.info("No upcoming events found");
            }

        } catch (Exception e) {
            logger.severe("Error listing calendar events: " + e.getMessage());
            e.printStackTrace();
        }
        return eventList;
    }

    public List<DriveFileDto> listDriveFiles(String accessTokenString) {
        List<DriveFileDto> fileList = new ArrayList<>();
        try {
            logger.info("Starting Google Drive files search with Access Token");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Drive service
            com.google.api.services.drive.Drive service = 
                new com.google.api.services.drive.Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // List files from Drive
            com.google.api.services.drive.model.FileList result = service.files()
                .list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name, mimeType, modifiedTime, size)")
                .execute();

            java.util.List<com.google.api.services.drive.model.File> files = result.getFiles();
            
            if (files != null && !files.isEmpty()) {
                for (com.google.api.services.drive.model.File file : files) {
                    String name = file.getName() != null ? file.getName() : "No name";
                    String mimeType = file.getMimeType() != null ? file.getMimeType() : "Unknown";
                    String modifiedTime = file.getModifiedTime() != null ? 
                        file.getModifiedTime().toString() : "Unknown";
                    String size = file.getSize() != null ? file.getSize().toString() + " bytes" : "Unknown";
                    
                    fileList.add(new DriveFileDto(name, mimeType, modifiedTime, size));
                }
                logger.info("Found " + fileList.size() + " files in Google Drive");
            } else {
                logger.info("No files found in Google Drive");
            }

        } catch (Exception e) {
            logger.severe("Error listing Google Drive files: " + e.getMessage());
            e.printStackTrace();
        }
        return fileList;
    }

    public List<String> uploadFileToDrive(String accessTokenString, String fileName, String mimeType, byte[] fileContent) {
        List<String> result = new ArrayList<>();
        try {
            logger.info("Starting file upload to Google Drive");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Drive service
            com.google.api.services.drive.Drive service = 
                new com.google.api.services.drive.Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // Create file metadata
            com.google.api.services.drive.model.File fileMetadata = 
                new com.google.api.services.drive.model.File();
            fileMetadata.setName(fileName);

            // Create input stream from byte array
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(fileContent);
            com.google.api.client.http.InputStreamContent mediaContent = 
                new com.google.api.client.http.InputStreamContent(mimeType, inputStream);

            // Upload file
            com.google.api.services.drive.model.File uploadedFile = service.files()
                .create(fileMetadata, mediaContent)
                .setFields("id, name, size")
                .execute();

            result.add("Archivo subido exitosamente: " + uploadedFile.getName());
            result.add("Tamaño: " + (uploadedFile.getSize() != null ? uploadedFile.getSize() + " bytes" : "Desconocido"));
            logger.info("File uploaded successfully: " + uploadedFile.getName());

        } catch (Exception e) {
            logger.severe("Error uploading file to Google Drive: " + e.getMessage());
            e.printStackTrace();
            result.add("Error al subir archivo: " + e.getMessage());
        }
        return result;
    }

    public List<String> createCalendarEvent(String accessTokenString, String summary, String description, String startDateTime, String endDateTime) {
        List<String> result = new ArrayList<>();
        try {
            logger.info("Creando evento en Google Calendar");
            
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

            // Create event
            com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event()
                .setSummary(summary)
                .setDescription(description);

            com.google.api.client.util.DateTime startDate = new com.google.api.client.util.DateTime(startDateTime);
            com.google.api.services.calendar.model.EventDateTime start = new com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(startDate)
                .setTimeZone("America/Argentina/Buenos_Aires");
            event.setStart(start);

            com.google.api.client.util.DateTime endDate = new com.google.api.client.util.DateTime(endDateTime);
            com.google.api.services.calendar.model.EventDateTime end = new com.google.api.services.calendar.model.EventDateTime()
                .setDateTime(endDate)
                .setTimeZone("America/Argentina/Buenos_Aires");
            event.setEnd(end);

            String calendarId = "primary";
            event = service.events().insert(calendarId, event).execute();
            
            result.add("Evento creado exitosamente: " + event.getHtmlLink());
            logger.info("Evento creado con ID: " + event.getId());

        } catch (Exception e) {
            logger.severe("Error al crear evento en Google Calendar: " + e.getMessage());
            e.printStackTrace();
            result.add("Error al crear evento: " + e.getMessage());
        }
        return result;
    }

    public List<TaskDto> listTasks(String accessTokenString) {
        List<TaskDto> taskList = new ArrayList<>();
        try {
            logger.info("Starting Google Tasks search with Access Token");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Tasks service
            com.google.api.services.tasks.Tasks service = 
                new com.google.api.services.tasks.Tasks.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // Get all task lists
            com.google.api.services.tasks.model.TaskLists taskLists = service.tasklists().list().execute();
            
            if (taskLists.getItems() != null && !taskLists.getItems().isEmpty()) {
                for (com.google.api.services.tasks.model.TaskList taskListItem : taskLists.getItems()) {
                    // Get tasks from this list
                    com.google.api.services.tasks.model.Tasks tasks = service.tasks()
                        .list(taskListItem.getId())
                        .setMaxResults(10)
                        .execute();
                    
                    if (tasks.getItems() != null) {
                        for (com.google.api.services.tasks.model.Task task : tasks.getItems()) {
                            String title = task.getTitle() != null ? task.getTitle() : "No title";
                            String status = task.getStatus() != null ? task.getStatus() : "needsAction";
                            String dueDate = task.getDue() != null ? task.getDue() : "No due date";
                            String notes = task.getNotes() != null ? task.getNotes() : "";
                            
                            taskList.add(new TaskDto(title, status, dueDate, notes));
                        }
                    }
                }
                logger.info("Found " + taskList.size() + " tasks in Google Tasks");
            } else {
                logger.info("No task lists found");
            }

        } catch (Exception e) {
            logger.severe("Error listing Google Tasks: " + e.getMessage());
            e.printStackTrace();
        }
        return taskList;
    }

    public List<String> createTask(String accessTokenString, String title, String notes, String dueDate) {
        List<String> result = new ArrayList<>();
        try {
            logger.info("Creando tarea en Google Tasks");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Tasks service
            com.google.api.services.tasks.Tasks service = 
                new com.google.api.services.tasks.Tasks.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // Get the default task list
            com.google.api.services.tasks.model.TaskLists taskLists = service.tasklists().list().execute();
            String taskListId = "@default";
            
            if (taskLists.getItems() != null && !taskLists.getItems().isEmpty()) {
                taskListId = taskLists.getItems().get(0).getId();
            }

            // Create task
            com.google.api.services.tasks.model.Task task = new com.google.api.services.tasks.model.Task()
                .setTitle(title)
                .setNotes(notes);

            if (dueDate != null && !dueDate.trim().isEmpty()) {
                try {
                    com.google.api.client.util.DateTime due = new com.google.api.client.util.DateTime(dueDate);
                    task.setDue(due.toStringRfc3339());
                } catch (Exception e) {
                    logger.warning("Formato de fecha inválido, creando tarea sin fecha de vencimiento");
                }
            }

            task = service.tasks().insert(taskListId, task).execute();
            
            result.add("Tarea creada exitosamente: " + task.getTitle());
            logger.info("Tarea creada con ID: " + task.getId());

        } catch (Exception e) {
            logger.severe("Error al crear tarea en Google Tasks: " + e.getMessage());
            e.printStackTrace();
            result.add("Error al crear tarea: " + e.getMessage());
        }
        return result;
    }

    public List<String> updateTaskStatus(String accessTokenString, String taskId, String status) {
        List<String> result = new ArrayList<>();
        try {
            logger.info("Actualizando estado de tarea en Google Tasks");
            
            // Build credentials with the access token
            AccessToken accessToken = new AccessToken(accessTokenString, null);
            GoogleCredentials credentials = GoogleCredentials.create(accessToken);

            // Build Tasks service
            com.google.api.services.tasks.Tasks service = 
                new com.google.api.services.tasks.Tasks.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                .setApplicationName("Google Auth API")
                .build();

            // Get the default task list
            com.google.api.services.tasks.model.TaskLists taskLists = service.tasklists().list().execute();
            String taskListId = "@default";
            
            if (taskLists.getItems() != null && !taskLists.getItems().isEmpty()) {
                taskListId = taskLists.getItems().get(0).getId();
            }

            // Get the task and update its status
            com.google.api.services.tasks.model.Task task = service.tasks().get(taskListId, taskId).execute();
            task.setStatus(status);
            
            if ("completed".equals(status)) {
                task.setCompleted(new com.google.api.client.util.DateTime(System.currentTimeMillis()).toStringRfc3339());
            }

            task = service.tasks().update(taskListId, taskId, task).execute();
            
            result.add("Estado de tarea actualizado a: " + task.getStatus());
            logger.info("Tarea actualizada con ID: " + task.getId());

        } catch (Exception e) {
            logger.severe("Error al actualizar tarea en Google Tasks: " + e.getMessage());
            e.printStackTrace();
            result.add("Error al actualizar tarea: " + e.getMessage());
        }
        return result;
    }
}

