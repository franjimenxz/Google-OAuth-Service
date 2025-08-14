# Google Services API

API para autenticación con Google y gestión de Calendar, Drive y Tasks.

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

**POST** `/api/auth/calendar/events/{userEmail}`
- Descripción: Crear nuevo evento en el calendario
- Body (form-data):
```
summary: Reunión de equipo
description: Reunión semanal de desarrollo
startDateTime: 2025-08-15T10:00:00-03:00
endDateTime: 2025-08-15T11:00:00-03:00
```

### Google Drive

**GET** `/api/auth/drive/files/{userEmail}`
- Descripción: Listar archivos recientes de Google Drive
- Ejemplo: `/api/auth/drive/files/juan@empresa.com`

### Google Tasks

**GET** `/api/auth/tasks/{userEmail}`
- Descripción: Listar todas las tareas del usuario
- Ejemplo: `/api/auth/tasks/juan@empresa.com`

**POST** `/api/auth/tasks/{userEmail}`
- Descripción: Crear nueva tarea
- Body (form-data):
```
title: Revisar código del proyecto
notes: Hacer code review antes del viernes
dueDate: 2025-08-16T18:00:00-03:00
```
Nota: Solo `title` es obligatorio, `notes` y `dueDate` son opcionales.

**PUT** `/api/auth/tasks/{userEmail}/{taskId}`
- Descripción: Actualizar estado de una tarea
- Body (form-data):
```
status: completed
```
Nota: Para obtener el taskId, primero hacer GET a `/tasks/{userEmail}`

### Información

**GET** `/api/auth/info`
- Descripción: Lista todos los endpoints disponibles con descripción

## Como usar

1. **Autenticarse**: Hacer POST a `/api/auth/google` con el código de OAuth
2. **Usar servicios**: Una vez autenticado, usar cualquier endpoint con tu email
3. **Formato fechas**: Usar formato ISO 8601: `2025-08-15T10:00:00-03:00`
4. **Parámetros POST**: Usar form-data o x-www-form-urlencoded en Postman

## Notas importantes

- Todos los endpoints (excepto autenticación) requieren que el usuario esté autenticado previamente
- El email usado en la URL debe coincidir con el usuario autenticado
- Los tokens se guardan en memoria, se pierden al reiniciar la aplicación
- Para usar Tasks, Calendar y Drive, asegúrate de que las APIs estén habilitadas en Google Cloud Console

## Ejecutar proyecto

```bash
mvn spring-boot:run
```

La API estará disponible en: http://localhost:8080