#  Servicio de Comunicación (HelpDesk)

> **Desarrollador Responsable:** **Gabriel Emilio Ponce Salazar**  
> **Estado:** ⏸️ **STANDBY (Fase Base Completada — Listo para Fase 2)**  
> **Rama asignada:** `feat/comunicacion`

Microservicio responsable del **canal de soporte técnico, registro de comentarios y centro de notificaciones** para usuarios y técnicos.

---

## ‍ Para el Desarrollador (Gabriel Emilio Ponce Salazar)
### ¿Qué está hecho hasta ahora?
- Entidades [`Comentario.java`](src/main/java/com/helpdesk/comunicacion/model/Comentario.java) (`ticketId`, `autorId`, `texto`) y [`Notificacion.java`](src/main/java/com/helpdesk/comunicacion/model/Notificacion.java) (`usuarioId`, `mensaje`, `leida`).
- Servicio [`ComunicacionService.java`](src/main/java/com/helpdesk/comunicacion/service/ComunicacionService.java) para crear/listar comentarios, listar/marcar leídas las notificaciones y procesar cambios de estado (`alCambiarEstado`).
- Controlador REST [`ComunicacionController.java`](src/main/java/com/helpdesk/comunicacion/controller/ComunicacionController.java) con rutas `GET/POST /comentarios`, `GET/PUT /notificaciones`, `POST /eventos/ticket-estado`.
- Base de datos PostgreSQL independiente y contenedor de **pgAdmin** listo en puerto `5054`.

### Tareas en Standby (Tu trabajo cuando reanudes):
1. **Recepción de Eventos (Fase 2):** Escuchar los eventos emitidos por **Ariel** (`TicketCreadoEvent`, `TicketEstadoCambiadoEvent`, `TicketAsignadoEvent`) para crear automáticamente una notificación dirigida al solicitante o al técnico sin necesidad de que te llamen por HTTP.
2. **Historial de Hilos:** Validar que los comentarios por `ticketId` salgan ordenados cronológicamente para que **Dylan** pueda pintarlos fluidamente en el chat/hilo del ticket en el Frontend.
3. **Filtros de Notificaciones:** Permitir obtener notificaciones no leídas de un usuario en particular (`GET /notificaciones?usuarioId=X&leida=false`).

---

##  ¿Qué hace este servicio? (Propósito de Negocio)
Es la vía de contacto continuo durante la resolución de un incidente:
- **Historial técnico de comentarios:** Permite a los técnicos y usuarios intercambiar notas, diagnósticos o comprobaciones asociadas al `ticketId`.
- **Bandeja de notificaciones:** Genera avisos automáticos al usuario cuando el estado de su ticket cambia (ej. cuando pasa a `EN_PROGRESO` o `RESUELTO`).
- **Control de lectura:** Permite al usuario marcar sus notificaciones como leídas.

---

##  Especificaciones de Red y Puertos
- **Aplicación Spring Boot:** Puerto `8084`
- **PostgreSQL Local:** Puerto `5435`
- **pgAdmin 4 (GUI Web):** Puerto `5054`
- **Base de Datos:** `comunicacion_db`
- **Usuario BD:** `comunicacion`
- **Contraseña BD:** `comunicacion`

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
> Si abres `http://localhost:8084/` en el navegador verás un error 404 de Spring Boot. Es normal porque es una **API REST**.

### 1. Para probar tu API REST:
Abre en tu navegador o en Postman el endpoint real:
 **[http://localhost:8084/comentarios](http://localhost:8084/comentarios)** o **[http://localhost:8084/notificaciones](http://localhost:8084/notificaciones)**  
*Respuesta esperada (JSON):* `[]`.

---

### 2. Para ver tu Base de Datos en pgAdmin 4:
Abre en tu navegador:
 **[http://localhost:5054](http://localhost:5054)**
1. Inicia sesión con: `admin@admin.com` / `admin`
2. Registra el servidor (**Servers**  **Register**  **Server...**):
   - **General:** Nombre: `Comunicacion DB`
   - **Connection:**
     - ️ **Host name/address:** `db` *(OJO: escribir exactamente la palabra `db`. Si pones `localhost` o `127.0.0.1` te dará error `Connection refused`)*
     - ️ **Port:** `5432` *(puerto interno, NO poner 5435)*
     - **Maintenance database:** `comunicacion_db`
     - **Username:** `comunicacion`
     - **Password:** `comunicacion`
     - Marca *Save password* y clic en **Save**.
3. Ve a `Schemas  public  Tables` para ver las tablas **`comentarios`** y **`notificaciones`**.

---

##  Detener el servicio
```bash
docker compose down
```
*(No uses `-v` para preservar los datos de la base).*
