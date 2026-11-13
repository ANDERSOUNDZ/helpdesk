#  Servicio de Identidad (HelpDesk)

> **Estado:** ⏸️ **STANDBY (Fase Base Operativa — Módulo Común del Equipo)**  
> **Rama asignada:** `feat/identidad`

Microservicio de **gestión de usuarios, roles de TI y control de acceso** de la Mesa de Ayuda.

---

## Para el Desarrollador (Anthony Marcel Orozco Junia)
### ¿Qué está hecho hasta ahora?
- Entidad [`Usuario.java`](src/main/java/com/helpdesk/identidad/model/Usuario.java) con `id`, `nombre`, `email`, enum [`Rol.java`](src/main/java/com/helpdesk/identidad/model/Rol.java) (`SOLICITANTE`, `TECNICO`, `ADMINISTRADOR`) y `activo`.
- Servicio [`UsuarioService.java`](src/main/java/com/helpdesk/identidad/service/UsuarioService.java) y Controlador REST [`UsuarioController.java`](src/main/java/com/helpdesk/identidad/controller/UsuarioController.java) con rutas completas para listar, crear y cambiar estado de activación.
- Base de datos PostgreSQL independiente y contenedor de **pgAdmin** listo en puerto `5053`.

### Tareas en Standby:
- Servir de catálogo de referencia para los IDs numéricos (`solicitanteId`, `tecnicoId`, `autorId`) utilizados en Tickets y Comunicación.
- Proveer los datos de usuarios cuando **Dylan** integre la vista de inicio de sesión o selección de usuario en el Frontend.

---

##  ¿Qué hace este servicio? (Propósito de Negocio)
Gestiona el catálogo de personas que interactúan en la mesa de ayuda corporativa:
- **Diferencia los roles de TI:**
  - `SOLICITANTE`: Empleado que reporta fallos o solicita requerimientos.
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

##  Autenticación y roles

Este servicio es el **único que conoce las credenciales**: guarda la contraseña cifrada (BCrypt) y emite el token (JWT).
Los demás servicios solo validan el token, sin llamar a identidad.

### Roles
`SOLICITANTE` (reporta incidentes), `TECNICO` (los atiende) y `ADMINISTRADOR` (gestiona usuarios y asignaciones).
Los usuarios que tenían rol `ESTUDIANTE` o `USUARIO` pasan a `SOLICITANTE` solos al arrancar (migración de la
restricción `usuarios_rol_check`).

### Está apagada por defecto
Con `AUTH_ENABLED=false` (lo normal) todo funciona como antes y no hace falta ningún token, así que cada persona puede
levantar su servicio sola. Se enciende con variables de entorno:

| Variable | Para qué | Notas |
|---|---|---|
| `AUTH_ENABLED` | `true` exige token en todos los endpoints (salvo `/auth/login` y `/actuator/health`) | por defecto `false` |
| `JWT_SECRET` | Clave con la que se firma el token | **mínimo 32 caracteres. Nunca en el repositorio (es público)** |
| `JWT_EXPIRACION_MIN` | Minutos de vida del token | por defecto `60` |
| `ADMIN_USUARIO`, `ADMIN_CLAVE`, `ADMIN_NOMBRE` | Crean el administrador inicial si no existe | la clave de al menos 8 caracteres |

Sin `JWT_SECRET`, `POST /auth/login` responde `503` con el motivo; no es un fallo del servicio.

### Endpoints
| Método y ruta | Quién | Qué hace |
|---|---|---|
| `POST /auth/login` | público | Recibe `{"username","clave"}` y devuelve `{token, tipo, expiraEn, usuario}` |
| `GET /usuarios`, `GET /usuarios/{id}` | cualquier usuario autenticado | Lista y consulta (nunca devuelve el hash ni la clave) |
| `POST`, `PUT`, `DELETE /usuarios` | `ADMINISTRADOR` | Crea, edita y elimina. Al crear se puede enviar `username` y `clave` para darle acceso |

El usuario (`username`) va en minúsculas (3 a 60 caracteres: letras, números, `.`, `-`, `_`) y es único; la clave exige 8
caracteres como mínimo. Un usuario sin `username` ni `clave` existe en el catálogo pero **no puede iniciar sesión**.

### Contrato del token (lo que validan los otros servicios)
JWT firmado con **HS256** y la clave `JWT_SECRET`, con estos datos:

| Dato | Valor |
|---|---|
| `iss` (emisor) | `helpdesk-identidad` |
| `sub` | el `username` |
| `uid` | el id del usuario |
| `nombre` | el nombre completo |
| `roles` | lista con el rol, por ejemplo `["TECNICO"]` |
| `exp` | vence a los `JWT_EXPIRACION_MIN` minutos (por defecto 60) |

### Prueba rápida
```bash
curl.exe -X POST localhost:8083/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"clave\":\"<tu clave>\"}"
curl.exe localhost:8083/usuarios -H "Authorization: Bearer <token>"
```

---

##  Detener el servicio
```bash
docker compose down
```
*(No uses `-v` para preservar los datos de la base).*
