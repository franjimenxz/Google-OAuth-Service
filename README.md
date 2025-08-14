# Google Services API

API para autenticación con Google y consulta de Calendar, Drive y Tasks.

## Endpoints Disponibles

### Autenticación

**POST** `/api/auth/google`
- Descripción: Autenticar con código de Google OAuth
- Body (JSON):
```json
{
  "code": "4/0AX4XfWjYour_Authorization_Code_Here"
}
```

**GET** `/api/auth/google/callback`
- Descripción: Callback para OAuth de Google (usado automáticamente por el navegador)

**GET** `/api/auth/status`
- Descripción: Verificar si la API está funcionando

### Google Calendar

**GET** `/api/auth/calendar/events/{userEmail}`
- Descripción: Listar próximos eventos del calendario
- Ejemplo: `/api/auth/calendar/events/juan@empresa.com`

### Google Drive

**GET** `/api/auth/drive/files/{userEmail}`
- Descripción: Listar archivos y carpetas de Google Drive con estructura jerárquica
- Ejemplo: `/api/auth/drive/files/juan@empresa.com`
- Respuesta: Estructura de árbol con carpetas y sus archivos anidados

**POST** `/api/auth/drive/upload/{userEmail}`
- Descripción: Subir archivo a Google Drive
- Body (JSON):
```json
{
  "fileName": "recibo_enero_2025.pdf",
  "mimeType": "application/pdf",
  "fileContent": "JVBERi0xLjQKJdPr6eEKMSAwIG9iago8PAovVHlwZSAv...",
  "folderId": "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms"
}
```
- Nota: fileContent debe ser el archivo codificado en base64
- Nota: folderId es opcional. Si no se especifica, se sube a la raíz de Drive

### Google Tasks

**GET** `/api/auth/tasks/{userEmail}`
- Descripción: Listar todas las listas de tareas con sus tareas organizadas jerárquicamente
- Ejemplo: `/api/auth/tasks/juan@empresa.com`
- Respuesta: Estructura de listas de tareas con tareas anidadas

### Información

**GET** `/api/auth/info`
- Descripción: Lista todos los endpoints disponibles con descripción

## Como usar

1. **Autenticarse**: Hacer POST a `/api/auth/google` con el código de OAuth
2. **Consultar servicios**: Una vez autenticado, usar cualquier endpoint GET con tu email
3. **Subir archivos**: Usar POST `/drive/upload/{userEmail}` con JSON y archivo en base64

## Notas importantes

- Todos los endpoints (excepto autenticación) requieren que el usuario esté autenticado previamente
- El email usado en la URL debe coincidir con el usuario autenticado
- Los tokens se guardan en memoria, se pierden al reiniciar la aplicación
- Para usar Tasks, Calendar y Drive, asegúrate de que las APIs estén habilitadas en Google Cloud Console
- Funciones disponibles: consulta de datos y upload de archivos a Drive

## Ejecutar proyecto

```bash
mvn spring-boot:run
```

La API estará disponible en: http://localhost:8080