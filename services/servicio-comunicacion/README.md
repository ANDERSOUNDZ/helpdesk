# Servicio de Comunicación (HelpDesk)

> **Desarrollador responsable:** Gabriel Emilio Ponce Salazar
> **Rama:** `feat/comunicacion`
> **Puerto:** `8084`
> **Base de datos:** `comunicacion_db` (PostgreSQL)

Microservicio responsable de la bitácora de soporte, las notificaciones, las encuestas de satisfacción y los adjuntos de los comentarios.

## Funcionalidades implementadas

- Comentarios con fecha de creación, autor y título del ticket.
- Notificaciones con tipo, ticket asociado, fecha, contador de no leídas y lectura masiva.
- Filtro por ticket, búsqueda por texto/autor/asunto y paginación de la bitácora.
- Encuesta CSAT de 1 a 5 cuando un ticket pasa a `RESUELTO`.
- Resumen de satisfacción para la pestaña de comunicación y el dashboard.
- Adjuntos PNG, JPG y PDF por comentario, con vista previa y descarga.
- Notificación automática al destinatario cuando se crea un comentario.
- Se conserva el contrato anterior de `GET /comentarios` y `GET /notificaciones` para no romper el diálogo de tickets.

## Endpoints

### Comentarios

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/comentarios?ticketId=` | Lista compatible con el frontend anterior. Devuelve un array. |
| `GET` | `/comentarios/paginados?ticketId=&q=&page=0&size=10&sort=creadoEn,desc` | Búsqueda y paginación. Devuelve una página de Spring. |
| `POST` | `/comentarios` | Crea un comentario y, si recibe `destinatarioId`, genera una notificación. |
| `DELETE` | `/comentarios/{id}` | Elimina el comentario y sus adjuntos. |

Ejemplo de creación:

```json
{
  "ticketId": 7,
  "autorId": 2,
  "autorNombre": "Ana Torres",
  "ticketTitulo": "No imprime la factura",
  "texto": "Revisé el controlador de impresión.",
  "destinatarioId": 3
}
```

`destinatarioId` es opcional. El emisor debe indicarlo porque este servicio no consulta tickets ni identidad.

### Notificaciones

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/notificaciones?usuarioId=&leida=&tipo=&ticketId=` | Lista filtrada. |
| `GET` | `/notificaciones/contador?usuarioId=` | Devuelve `total` y `noLeidas`. |
| `PUT` | `/notificaciones/leidas?usuarioId=` | Marca todas las no leídas del usuario. Devuelve `204`. |
| `PUT` | `/notificaciones/{id}/leida` | Marca una notificación como leída. |

Tipos: `TICKET_ESTADO`, `COMENTARIO_NUEVO`, `ENCUESTA_SATISFACCION`, `SISTEMA`.

### Encuestas CSAT

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/encuestas/{ticketId}` | Consulta la encuesta del ticket. |
| `POST` | `/encuestas/{ticketId}/respuesta` | Guarda una puntuación de 1 a 5. |
| `GET` | `/encuestas/resumen` | Promedio, respondidas, pendientes y distribución. |

El evento `POST /eventos/ticket-estado` con `estadoNuevo = "RESUELTO"` crea la encuesta y la notificación CSAT una sola vez por ticket.

### Adjuntos

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/comentarios/{id}/adjuntos` | Multipart con el campo `archivos`. Hasta 3 archivos de 5 MB. |
| `GET` | `/comentarios/{id}/adjuntos` | Metadata de adjuntos del comentario. |
| `GET` | `/adjuntos/{id}` | Vista previa inline. |
| `GET` | `/adjuntos/{id}/descargar` | Descarga con el nombre original. |
| `DELETE` | `/adjuntos/{id}` | Elimina metadata y archivo. |

Tipos permitidos: `image/png`, `image/jpeg`, `application/pdf`.

## Configuración de adjuntos

```properties
app.adjuntos.directorio=${ADJUNTOS_DIR:./datos/adjuntos}
app.adjuntos.max-tamano-bytes=${ADJUNTOS_MAX_BYTES:5242880}
app.adjuntos.max-archivos=${ADJUNTOS_MAX_ARCHIVOS:3}
app.adjuntos.tipos-permitidos=${ADJUNTOS_TIPOS:image/png,image/jpeg,application/pdf}

spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=20MB
```

En Docker, el servicio de comunicación usa el volumen `adjuntos` montado en `/app/adjuntos`.

## Flujo CSAT

1. `servicio-tickets` (o el frontend, mientras no exista el evento automático) envía:

```json
{
  "ticketId": 7,
  "estadoNuevo": "RESUELTO",
  "solicitanteId": 3
}
```

a `POST /eventos/ticket-estado`.
2. Se crea una notificación de estado.
3. Se crea una encuesta pendiente y una notificación `ENCUESTA_SATISFACCION`.
4. El solicitante responde desde la pestaña de comunicación.
5. El resumen aparece en la pestaña de comunicación y en el dashboard.

## Notificaciones automáticas al comentar

El backend no puede deducir el destinatario porque no consulta `servicio-tickets`. El frontend de comunicación y el diálogo de tickets envían `destinatarioId`:

- Si comenta el técnico, se notifica al solicitante.
- Si comenta el solicitante, se notifica al técnico asignado.
- Si el destinatario coincide con el autor, no se genera notificación.

## Integración pendiente

- `servicio-tickets` todavía no emite el evento al resolver por sí solo. Mientras tanto, el frontend de tickets lo envía después de resolver.
- No hay autenticación real. Los IDs de autor y destinatario se confían desde el cliente; es una simulación para la demo.
- El dashboard principal consume `/encuestas/resumen`; el dueño del dashboard puede extender la visualización.

## Ejecución

```bash
cd services/servicio-comunicacion
./mvnw spring-boot:run
```

En Windows:

```powershell
cd services/servicio-comunicacion
.\mvnw.cmd spring-boot:run
```

Con Docker desde la raíz:

```bash
docker compose up -d --build servicio-comunicacion
```

## Pruebas

```powershell
cd services/servicio-comunicacion
.\mvnw.cmd test
```

Pruebas incluidas:

- Creación de comentarios y notificaciones.
- Filtros y paginación.
- Contador y lectura masiva.
- Evento `RESUELTO`, encuesta y resumen CSAT.
- Validación de adjuntos, almacenamiento y descarga.

## Postman

La colección está en:

`services/servicio-comunicacion/postman/servicio-comunicacion.postman_collection.json`

Carpetas: Comentarios, Notificaciones, Encuestas, Adjuntos y Eventos.


---

## Autenticación y permisos por rol

Valida el token (JWT) que emite `servicio-identidad`, **sin llamarlo**. Está **apagada por defecto** (`AUTH_ENABLED=false`):
con el interruptor apagado todo funciona como antes y no hace falta ningún token. Se enciende con variables de entorno:
`AUTH_ENABLED=true` y `JWT_SECRET` (la **misma clave** que usa identidad, mínimo 32 caracteres; nunca en el repositorio, que es público).

| Endpoint | Quién puede |
|---|---|
| Comentarios, adjuntos, notificaciones, encuestas y `POST /eventos/ticket-estado` | cualquier usuario autenticado |
| `DELETE /comentarios/{id}`, `DELETE /adjuntos/{id}` | `ADMINISTRADOR` |
| `GET /actuator/health` | público |

Sin token o con un token inválido, vencido o de otro emisor responde `401`; con un rol sin permiso, `403`.
La regla está en `config/SeguridadConfig.java` y se prueba en `SeguridadConfigTest`.

Nota para el frontend: con la autenticación encendida, descargar un adjunto con un enlace directo (`<a href>` o `<img src>`)
no envía el token y recibe `401`; hay que pedirlo con `HttpClient` (con el interceptor) y abrirlo como `blob`.
