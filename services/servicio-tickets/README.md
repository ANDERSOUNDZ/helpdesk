#  Servicio de Tickets (HelpDesk)

> **Desarrollador Responsable:** **Ariel Martín Guerrero Sandoval**  
> **Estado:** **OPERATIVO** — ciclo completo de tickets, SLA, historial, categorías, horas y reportes. Pendiente: broker RabbitMQ en el entorno integrado.  
> **Rama asignada:** `feat/tickets`

Microservicio **núcleo del sistema** encargado del ciclo de vida y gestión de incidentes y solicitudes de soporte técnico de TI.

---

## ‍ Para el Desarrollador (Ariel Martín Guerrero Sandoval)
### ¿Qué está hecho hasta ahora?
- Entidad [`Ticket.java`](src/main/java/com/helpdesk/tickets/model/Ticket.java) con `id`, `titulo`, `descripcion`, `prioridad` (`BAJA`, `MEDIA`, `ALTA`), `estado` (`ABIERTO`, `EN_PROGRESO`, `RESUELTO`), `solicitanteId`, `tecnicoId`, `categoriaId` y fechas de ciclo de vida (`creadoEn`, `asignadoEn`, `resueltoEn`, `fechaLimite`, `recordatorioEnviadoEn`). El campo calculado `vencido` no se guarda.
- Servicio [`TicketService.java`](src/main/java/com/helpdesk/tickets/service/TicketService.java) con métodos para crear, asignar técnico, resolver, reabrir, editar y eliminar tickets, con **reglas de negocio validadas**:
  - No se resuelve un ticket que no esté `EN_PROGRESO` o que no tenga técnico.
  - No se asigna ni se resuelve dos veces un ticket ya `RESUELTO`.
  - Solo se reabre un ticket `RESUELTO`.
  - Título obligatorio (máx. 200) y descripción (máx. 1000); IDs de solicitante y técnico positivos.
  - Asignar al mismo técnico es idempotente.
  - La categoría debe existir; al cambiar la prioridad se recalcula el SLA.
- **SLA y fechas:** al crear un ticket se calcula `fechaLimite` según la prioridad (ALTA 8 h, MEDIA 24 h, BAJA 72 h, configurables). `GET /tickets/vencidos` y el filtro `vencidos=true` listan los tickets abiertos con la fecha límite pasada.
- **Recordatorios por SLA:** [`SlaService.java`](src/main/java/com/helpdesk/tickets/service/SlaService.java) revisa cada 15 minutos (cron configurable) los tickets por vencer o vencidos y publica `ticket.sla-por-vencer` / `ticket.sla-vencido`. Marca `recordatorioEnviadoEn` para no repetir el aviso.
- **Filtros, búsqueda y paginación:** `GET /tickets` acepta `estado`, `prioridad`, `tecnicoId`, `solicitanteId`, `categoriaId`, `q` (texto en título/descripción) y `vencidos`. `GET /tickets/paginado` agrega `page` y `size` (máx. 100).
- **Historial de cambios:** [`HistorialService.java`](src/main/java/com/helpdesk/tickets/service/HistorialService.java) guarda en `historial_tickets` cada cambio de estado, técnico, título, descripción, prioridad, categoría y horas, con fecha y usuario (claim `uid` del JWT).
- **Categorías:** [`CategoriaService.java`](src/main/java/com/helpdesk/tickets/service/CategoriaService.java) con CRUD en `/categorias`. No se puede eliminar una categoría con tickets asociados.
- **Registro de horas:** [`HorasService.java`](src/main/java/com/helpdesk/tickets/service/HorasService.java) guarda minutos dedicados por técnico en `registros_horas` y devuelve el total en minutos y horas.
- **Acciones en lote:** `POST /tickets/lote/asignar` y `POST /tickets/lote/resolver` procesan hasta 100 tickets y reportan cuáles fallaron sin detener el resto.
- **Exportación CSV:** `GET /tickets/export.csv` exporta los tickets filtrados (UTF-8 con BOM, comas y comillas escapadas).
- **Emisión de eventos (Fase 2, lado tickets):** al crear, asignar, resolver o reabrir se publican `TicketCreadoEvent`, `TicketAsignadoEvent` y `TicketEstadoCambiadoEvent` mediante [`PublicadorEventos`](src/main/java/com/helpdesk/tickets/evento/PublicadorEventos.java). Por defecto `EVENTOS_PUBLICADOR=log` (no necesita broker); con `EVENTOS_PUBLICADOR=rabbit` publica en el exchange `helpdesk.eventos` con las routing keys `ticket.creado`, `ticket.asignado`, `ticket.estado-cambiado`, `ticket.sla-por-vencer` y `ticket.sla-vencido`.
- Controladores REST [`TicketController.java`](src/main/java/com/helpdesk/tickets/controller/TicketController.java) y [`CategoriaController.java`](src/main/java/com/helpdesk/tickets/controller/CategoriaController.java) con las rutas de la tabla de endpoints.
- Base de datos PostgreSQL independiente y contenedor de **pgAdmin** listo en puerto `5050`.

### Endpoints principales

| Método y ruta | Descripción |
|---|---|
| `GET /tickets` | Lista con filtros (`estado`, `prioridad`, `tecnicoId`, `solicitanteId`, `categoriaId`, `q`, `vencidos`) |
| `GET /tickets/paginado` | Igual que el anterior más `page` y `size` |
| `GET /tickets/vencidos` | Tickets abiertos con SLA vencido |
| `GET /tickets/export.csv` | Exportación CSV de los filtros aplicados |
| `GET /tickets/{id}` | Detalle de un ticket |
| `GET /tickets/{id}/historial` | Bitácora de cambios del ticket |
| `POST /tickets` | Crear ticket (calcula estado inicial y SLA) |
| `PUT /tickets/{id}/asignar/{tecnicoId}` | Asignar técnico |
| `PUT /tickets/{id}/resolver` | Resolver ticket |
| `PUT /tickets/{id}/reabrir` | Reabrir ticket resuelto |
| `PUT /tickets/{id}` | Editar ticket |
| `DELETE /tickets/{id}` | Eliminar ticket |
| `POST /tickets/lote/asignar` | Asignar un técnico a varios tickets |
| `POST /tickets/lote/resolver` | Resolver varios tickets |
| `GET /tickets/{id}/horas` | Resumen y detalle de horas |
| `POST /tickets/{id}/horas` | Registrar horas (`tecnicoId`, `minutos`, `descripcion`) |
| `DELETE /tickets/{id}/horas/{registroId}` | Eliminar un registro de horas |
| `GET /categorias`, `GET /categorias/{id}` | Listar y consultar categorías |
| `POST /categorias`, `PUT /categorias/{id}`, `DELETE /categorias/{id}` | Administrar categorías |

### Configuración de SLA y eventos
| Variable | Por defecto | Uso |
|---|---|---|
| `SLA_HORAS_ALTA` / `SLA_HORAS_MEDIA` / `SLA_HORAS_BAJA` | `8` / `24` / `72` | Horas de SLA por prioridad |
| `SLA_ANTICIPACION_MINUTOS` | `60` | Antelación del recordatorio "por vencer" |
| `SLA_RECORDATORIOS_ENABLED` | `true` | Apaga el job de recordatorios |
| `SLA_RECORDATORIOS_CRON` | `0 */15 * * * *` | Frecuencia del job (cada 15 min) |
| `EVENTOS_PUBLICADOR` | `log` | `log` o `rabbit` |

### Pendiente (próximas tareas):
1. **Broker en el entorno integrado:** agregar RabbitMQ al `docker-compose.yml` de la raíz (archivo compartido: rama `chore/<tema>` con PR aprobado por Miguel) y acordar colas y consumidores con **Gabriel** (comunicación) y **Miguel** (estadísticas). Mientras tanto el publicador por defecto es `log`.
2. **Contratos con Dylan (API Gateway / Frontend):** asegurar que las respuestas JSON (incluidos los nuevos filtros, paginación, historial, horas y CSV) coincidan con las interfaces del cliente web.
3. **Mejoras futuras:** notificaciones en tiempo real desde el frontend y métricas agregadas de SLA para el servicio de estadísticas.

---

##  ¿Qué hace este servicio? (Propósito de Negocio)
Es el corazón operativo del HelpDesk. Sus responsabilidades son:
- **Registrar y generar folios únicos** para incidentes de TI con niveles de prioridad (`BAJA`, `MEDIA`, `ALTA`).
- **Gestionar la máquina de estados del incidente:** `ABIERTO` → `EN_PROGRESO` → `RESUELTO`.
- **Asignar técnicos de soporte** a cada caso (`PUT /tickets/{id}/asignar/{tecnicoId}`).
- **Garantizar la resolución formal** del problema antes del cierre.
- **Medir tiempos:** SLA por prioridad, avisos de vencimiento, historial de cambios y horas dedicadas por técnico.
- **Facilitar la operación:** filtros, búsqueda, paginación, acciones en lote y exportación CSV.

---

##  Especificaciones de Red y Puertos
- **Aplicación Spring Boot:** Puerto `8080`
- **PostgreSQL Local:** Puerto `5433`
- **pgAdmin 4 (GUI Web):** Puerto `5050`
- **Base de Datos:** `tickets_db`
- **Usuario BD:** `tickets`
- **Contraseña BD:** `tickets`

---

##  Cómo levantarlo con Docker

Ejecuta desde esta carpeta:
```bash
docker compose up -d --build
```

Verifica el estado de los 3 contenedores (`app`, `db`, `pgadmin`):
```bash
docker compose ps
```

---

##  ¿A DÓNDE DEBES IR Y QUÉ DEBES HACER AL LEVANTAR EL SERVICIO?

> ️ **¡OJO CON EL ERROR 404 / WHITELABEL ERROR PAGE!**  
> Si abres `http://localhost:8080/` en el navegador verás un error 404 de Spring Boot. Es normal porque es una **API REST**.

### 1. Para probar tu API REST:
Abre en tu navegador o en Postman el endpoint real:
 **[http://localhost:8080/tickets](http://localhost:8080/tickets)**  
*Respuesta esperada (JSON):* `[]` (o la lista de tickets).

O por terminal:
```bash
curl.exe http://localhost:8080/tickets
```

---

### 2. Para ver tu Base de Datos en pgAdmin 4:
Abre en tu navegador:
 **[http://localhost:5050](http://localhost:5050)**
1. Inicia sesión con: `admin@admin.com` / `admin`
2. Registra el servidor (**Servers**  **Register**  **Server...**):
   - **General:** Nombre: `Tickets DB`
   - **Connection:**
     - ️ **Host name/address:** `db` *(OJO: escribir exactamente la palabra `db`. Si pones `localhost` o `127.0.0.1` te dará error `Connection refused`)*
     - ️ **Port:** `5432` *(puerto interno, NO poner 5433)*
     - **Maintenance database:** `tickets_db`
     - **Username:** `tickets`
     - **Password:** `tickets`
     - Marca *Save password* y clic en **Save**.
3. Ve a `Schemas  public  Tables` para ver las tablas **`tickets`**, **`categorias`**, **`historial_tickets`** y **`registros_horas`**.

---

##  Detener el servicio
```bash
docker compose down
```
*(No uses `-v` para preservar los datos de la base).*


---

##  Autenticación y permisos por rol

Valida el token (JWT) que emite `servicio-identidad`, **sin llamarlo**. Está **apagada por defecto** (`AUTH_ENABLED=false`):
con el interruptor apagado todo funciona como antes y no hace falta ningún token. Se enciende con variables de entorno:
`AUTH_ENABLED=true` y `JWT_SECRET` (la **misma clave** que usa identidad, mínimo 32 caracteres; nunca en el repositorio, que es público).

| Endpoint | Quién puede |
|---|---|
| `GET /tickets`, `GET /tickets/{id}`, `POST /tickets` | autenticado (`POST /tickets`: `SOLICITANTE`, `ADMINISTRADOR`) |
| `GET /tickets/paginado`, `GET /tickets/vencidos`, `GET /tickets/{id}/historial`, `GET /tickets/{id}/horas` | cualquier usuario autenticado |
| `GET /tickets/export.csv` | `ADMINISTRADOR` |
| `PUT /tickets/{id}/reabrir` | cualquier usuario autenticado |
| `PUT /tickets/{id}/resolver`, `PUT /tickets/{id}/asignar/{tecnicoId}` | `TECNICO`, `ADMINISTRADOR` |
| `PUT /tickets/{id}` (editar), `DELETE /tickets/{id}` | `ADMINISTRADOR` |
| `POST /tickets/lote/asignar`, `POST /tickets/lote/resolver` | `TECNICO`, `ADMINISTRADOR` |
| `POST /tickets/{id}/horas` | `TECNICO`, `ADMINISTRADOR` |
| `DELETE /tickets/{id}/horas/{registroId}` | `ADMINISTRADOR` |
| `GET /categorias`, `GET /categorias/{id}` | cualquier usuario autenticado |
| `POST /categorias`, `PUT /categorias/{id}`, `DELETE /categorias/{id}` | `ADMINISTRADOR` |
| `GET /actuator/health` | público |

Sin token o con un token inválido, vencido o de otro emisor responde `401`; con un rol sin permiso, `403`.
La regla está en `config/SeguridadConfig.java` y se prueba en `SeguridadConfigTest`.
