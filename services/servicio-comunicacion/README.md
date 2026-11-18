# Servicio de Comunicacion (HelpDesk)

> **Desarrollador responsable:** Gabriel Emilio Ponce Salazar
> **Rama:** `feat/comunicacion-rediseno`
> **Puerto:** `8084`
> **Base de datos:** `comunicacion_db` (PostgreSQL)

Microservicio responsable de la bitacora de soporte y las notificaciones de los tickets.

## Funcionalidades implementadas

- Comentarios con fecha de creacion, autor y titulo del ticket.
- Notificaciones con tipo, ticket asociado, fecha, contador de no leidas y lectura masiva.
- Busqueda por texto/autor/asunto y paginacion de la bitacora.
- Separacion de la bitacora en tickets **en curso** y **resueltos** desde el frontend.
- Notificacion automatica al destinatario cuando se crea un comentario.
- Se conserva el contrato anterior de `GET /comentarios` y `GET /notificaciones` para no romper el dialogo de tickets.

## Endpoints

### Comentarios

| Metodo | Ruta | Descripcion |
|---|---|---|
| `GET` | `/comentarios?ticketId=` | Lista compatible con el frontend anterior. Devuelve un array. |
| `GET` | `/comentarios/paginados?ticketId=&ticketIds=&q=&page=0&size=10&sort=creadoEn,desc` | Busqueda y paginacion. Devuelve una pagina de Spring. |
| `POST` | `/comentarios` | Crea un comentario y, si recibe `destinatarioId`, genera una notificacion. |
| `DELETE` | `/comentarios/{id}` | Elimina el comentario. |

`ticketIds` permite limitar la bitacora a un conjunto de tickets. El frontend lo usa para mostrar solo los comentarios de tickets en curso o solo los de tickets resueltos. Ejemplo:

```http
GET /comentarios/paginados?ticketIds=7,8,9&page=0&size=10
```

Ejemplo de creacion:

```json
{
  "ticketId": 7,
  "autorId": 2,
  "autorNombre": "Ana Torres",
  "ticketTitulo": "No imprime la factura",
  "texto": "Revise el controlador de impresion.",
  "destinatarioId": 3
}
```

`destinatarioId` es opcional. El emisor debe indicarlo porque este servicio no consulta tickets ni identidad.

### Notificaciones

| Metodo | Ruta | Descripcion |
|---|---|---|
| `GET` | `/notificaciones?usuarioId=&leida=&tipo=&ticketId=` | Lista filtrada. |
| `GET` | `/notificaciones/contador?usuarioId=` | Devuelve `total` y `noLeidas`. |
| `PUT` | `/notificaciones/leidas?usuarioId=` | Marca todas las no leidas del usuario. Devuelve `204`. |
| `PUT` | `/notificaciones/{id}/leida` | Marca una notificacion como leida. |

Tipos: `TICKET_ESTADO`, `COMENTARIO_NUEVO`, `SISTEMA`.

### Eventos

| Metodo | Ruta | Descripcion |
|---|---|---|
| `POST` | `/eventos/ticket-estado` | Recibe el cambio de estado y crea un aviso para el solicitante. |

Ejemplo:

```json
{
  "ticketId": 7,
  "estadoNuevo": "RESUELTO",
  "solicitanteId": 3
}
```

## Notificaciones automaticas al comentar

El backend no puede deducir el destinatario porque no consulta `servicio-tickets`. El frontend de comunicacion y el dialogo de tickets envian `destinatarioId`:

- Si comenta el tecnico, se notifica al solicitante.
- Si comenta el solicitante, se notifica al tecnico asignado.
- Si el destinatario coincide con el autor, no se genera notificacion.

## Integracion pendiente

- `servicio-tickets` todavia no emite el evento al resolver por si solo. Mientras tanto, el frontend de tickets lo envia despues de resolver.
- No hay autenticacion real. Los IDs de autor y destinatario se confian desde el cliente; es una simulacion para la demo.

## Datos anteriores

Las encuestas de satisfaccion y los adjuntos se retiraron del servicio. Las tablas antiguas no se borran automaticamente para no destruir datos existentes, pero el codigo ya no las consulta. Al arrancar se eliminan los avisos historicos de tipo `ENCUESTA_SATISFACCION` para que no queden valores de enum obsoletos.

## Ejecucion

```bash
cd services/servicio-comunicacion
./mvnw spring-boot:run
```

En Windows:

```powershell
cd services/servicio-comunicacion
.\mvnw.cmd spring-boot:run
```

Con Docker desde la raiz:

```bash
docker compose up -d --build servicio-comunicacion
```

## Pruebas

```powershell
cd services/servicio-comunicacion
.\mvnw.cmd test
```

Pruebas incluidas:

- Creacion de comentarios y notificaciones.
- Filtros por ticket y por lista de tickets.
- Busqueda y paginacion.
- Contador y lectura masiva.
- Evento de cambio de estado.

## Postman

La coleccion esta en:

`services/servicio-comunicacion/postman/servicio-comunicacion.postman_collection.json`

Carpetas: Comentarios, Notificaciones y Eventos.

---

## Autenticacion y permisos por rol

Valida el token (JWT) que emite `servicio-identidad`, **sin llamarlo**. Esta **apagada por defecto** (`AUTH_ENABLED=false`):
con el interruptor apagado todo funciona como antes y no hace falta ningun token. Se enciende con variables de entorno:
`AUTH_ENABLED=true` y `JWT_SECRET` (la **misma clave** que usa identidad, minimo 32 caracteres; nunca en el repositorio, que es publico).

| Endpoint | Quien puede |
|---|---|
| Comentarios, notificaciones y `POST /eventos/ticket-estado` | cualquier usuario autenticado |
| `DELETE /comentarios/{id}` | `ADMINISTRADOR` |
| `GET /actuator/health` | publico |

Sin token o con un token invalido, vencido o de otro emisor responde `401`; con un rol sin permiso, `403`.
La regla esta en `config/SeguridadConfig.java` y se prueba en `SeguridadConfigTest`.
