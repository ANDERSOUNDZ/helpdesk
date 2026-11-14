#  Servicio de Tickets (HelpDesk)

> **Desarrollador Responsable:** **Ariel Martín Guerrero Sandoval**  
> **Estado:** **OPERATIVO** — ciclo completo de tickets con permisos por rol. Pendiente: eventos hacia otros servicios (Fase 2).  
> **Rama asignada:** `feat/tickets`

Microservicio **núcleo del sistema** encargado del ciclo de vida y gestión de incidentes y solicitudes de soporte técnico de TI.

---

## ‍ Para el Desarrollador (Ariel Martín Guerrero Sandoval)
### ¿Qué está hecho hasta ahora?
- Entidad [`Ticket.java`](src/main/java/com/helpdesk/tickets/model/Ticket.java) con `id`, `titulo`, `descripcion`, `prioridad` (`BAJA`, `MEDIA`, `ALTA`), `estado` (`ABIERTO`, `EN_PROGRESO`, `RESUELTO`), `solicitanteId`, `tecnicoId`.
- Servicio [`TicketService.java`](src/main/java/com/helpdesk/tickets/service/TicketService.java) con métodos para crear, asignar técnico, resolver, reabrir, editar y eliminar tickets.
- Controlador REST [`TicketController.java`](src/main/java/com/helpdesk/tickets/controller/TicketController.java) con rutas `GET /tickets`, `GET /tickets/{id}`, `POST /tickets`, `PUT /tickets/{id}/asignar/{tecnicoId}`, `PUT /tickets/{id}/resolver`, `PUT /tickets/{id}/reabrir`, `PUT /tickets/{id}` y `DELETE /tickets/{id}`.
- Base de datos PostgreSQL independiente y contenedor de **pgAdmin** listo en puerto `5050`.

### Pendiente (próximas tareas):
1. **Validación de reglas de negocio:** Verificar que no se pueda resolver un ticket que no esté en progreso o que no tenga técnico asignado.
2. **Emisión de Eventos (Fase 2):** Cuando un ticket se cree (`POST /tickets`), se asigne o se resuelva, publicar el evento (`TicketCreadoEvent`, `TicketEstadoCambiadoEvent`) para que **Gabriel** (Comunicación) y **Miguel** (Estadísticas) reciban la notificación automáticamente.
3. **Contratos con Dylan (API Gateway / Frontend):** Asegurar que las respuestas JSON coincidan con las interfaces esperadas por el cliente web.

---

##  ¿Qué hace este servicio? (Propósito de Negocio)
Es el corazón operativo del HelpDesk. Sus responsabilidades son:
- **Registrar y generar folios únicos** para incidentes de TI con niveles de prioridad (`BAJA`, `MEDIA`, `ALTA`).
- **Gestionar la máquina de estados del incidente:** `ABIERTO`  `EN_PROGRESO`  `RESUELTO`.
- **Asignar técnicos de soporte** a cada caso (`PUT /tickets/{id}/asignar/{tecnicoId}`).
- **Garantizar la resolución formal** del problema antes del cierre.

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
3. Ve a `Schemas  public  Tables` para ver las tablas **`tickets`** y **`categorias`**.

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
| `GET /tickets`, `GET /tickets/{id}`, `POST /tickets` | cualquier usuario autenticado |
| `PUT /tickets/{id}/reabrir` | cualquier usuario autenticado |
| `PUT /tickets/{id}/resolver` | `TECNICO`, `ADMINISTRADOR` |
| `PUT /tickets/{id}/asignar/{tecnicoId}` | `ADMINISTRADOR` |
| `PUT /tickets/{id}` (editar), `DELETE /tickets/{id}` | `ADMINISTRADOR` |
| `GET /actuator/health` | público |

Sin token o con un token inválido, vencido o de otro emisor responde `401`; con un rol sin permiso, `403`.
La regla está en `config/SeguridadConfig.java` y se prueba en `SeguridadConfigTest`.
