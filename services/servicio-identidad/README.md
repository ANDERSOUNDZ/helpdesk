#  Servicio de Identidad (HelpDesk)

> **Estado:** ⏸️ **STANDBY (Fase Base Operativa — Módulo Común del Equipo)**  
> **Rama asignada:** `feat/identidad`

Microservicio de **gestión de usuarios, roles de TI y control de acceso** de la Mesa de Ayuda.

---

##  Para el Equipo de Desarrollo
### ¿Qué está hecho hasta ahora?
- Entidad [`Usuario.java`](src/main/java/com/helpdesk/identidad/model/Usuario.java) con `id`, `nombre`, `email`, enum [`Rol.java`](src/main/java/com/helpdesk/identidad/model/Rol.java) (`USUARIO`, `TECNICO`, `ADMINISTRADOR`) y `activo`.
- Servicio [`UsuarioService.java`](src/main/java/com/helpdesk/identidad/service/UsuarioService.java) y Controlador REST [`UsuarioController.java`](src/main/java/com/helpdesk/identidad/controller/UsuarioController.java) con rutas completas para listar, crear y cambiar estado de activación.
- Base de datos PostgreSQL independiente y contenedor de **pgAdmin** listo en puerto `5053`.

### Tareas en Standby:
- Servir de catálogo de referencia para los IDs numéricos (`solicitanteId`, `tecnicoId`, `autorId`) utilizados en Tickets y Comunicación.
- Proveer los datos de usuarios cuando **Dylan** integre la vista de inicio de sesión o selección de usuario en el Frontend.

---

##  ¿Qué hace este servicio? (Propósito de Negocio)
Gestiona el catálogo de personas que interactúan en la mesa de ayuda corporativa:
- **Diferencia los roles de TI:**
  - `USUARIO`: Empleado que reporta fallos o solicita requerimientos.
  - `TECNICO`: Especialista de soporte asignado para diagnosticar y reparar incidentes.
  - `ADMINISTRADOR`: Supervisor que gestiona la carga operativa y configuración.
- **Control de estado:** Permite activar o desactivar usuarios en el sistema.

---

##  Especificaciones de Red y Puertos
- **Aplicación Spring Boot:** Puerto `8083`
- **PostgreSQL Local:** Puerto `5434`
- **pgAdmin 4 (GUI Web):** Puerto `5053`
- **Base de Datos:** `identidad_db`
- **Usuario BD:** `identidad`
- **Contraseña BD:** `identidad`

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

### Comprobación rápida de funcionamiento (Healthcheck)
```bash
curl.exe http://localhost:8083/usuarios
```
*Respuesta esperada:* `[]` (o la lista de usuarios registrados).

---

##  Conexión a pgAdmin 4
1. Abre en tu navegador: [http://localhost:5053](http://localhost:5053)
2. Inicia sesión con:
   - **Email:** `admin@admin.com`
   - **Password:** `admin`
3. Registra el servidor de base de datos (**Servers**  **Register**  **Server...**):
   - **Pestaña General:** Nombre: `Identidad DB`
   - **Pestaña Connection:**
     - ️ **Host name/address:** `db` *(OJO: escribir exactamente la palabra `db`. Si pones `localhost` o `127.0.0.1` te dará error `Connection refused`)*
     - ️ **Port:** `5432` *(puerto interno, NO poner 5434)*
     - **Maintenance database:** `identidad_db`
     - **Username:** `identidad`
     - **Password:** `identidad`
     - Marca *Save password* y haz clic en **Save**.
4. Ve a `Schemas  public  Tables` para ver las tablas **`usuarios`** y **`departamentos`**.

---

##  Detener el servicio
```bash
docker compose down
```
*(No uses `-v` para preservar los datos de la base).*
