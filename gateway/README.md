#  API Gateway (HelpDesk) — [STANDBY]

> **Desarrollador Responsable:** **Dylan Miguel Vargas Onofa**  
> **Estado:** ⏳ **PENDIENTE (aún no creado)** — ya existe lo que necesita: Eureka (`:8081`) y login con JWT.  
> **Puerto asignado:** `8000`  
> **Rama asignada:** `feat/gateway`

---

##  Objetivo del Componente
El **API Gateway** es la puerta de enlace única y transparente hacia todo el backend distribuido de la Mesa de Ayuda de TI. Permite que el cliente web (Frontend) se comunique con una sola dirección (`http://localhost:8000/api/...`) sin tener que conocer los puertos internos de cada microservicio.

---

##  Tareas en Standby para Dylan
Cuando inicies el desarrollo de este módulo con el asistente, aquí está el punto de partida:

1. **Creación del Proyecto:**
   - Crear una aplicación Spring Boot con **Spring Cloud Gateway** y dependencias de Reactive Gateway.
2. **Configuración de Rutas (`application.yml`):**
   - Registrar el gateway en **Eureka** (`http://localhost:8081/eureka/`) y enrutar **por nombre de servicio**, sin escribir puertos:
     - `/api/tickets/**` → `lb://servicio-tickets` (`/tickets/**`)
     - `/api/usuarios/**` y `/api/auth/**` → `lb://servicio-identidad` (`/usuarios/**` y `/auth/**`)
     - `/api/comentarios/**`, `/api/notificaciones/**`, `/api/adjuntos/**`, `/api/encuestas/**` → `lb://servicio-comunicacion`
     - `/api/estadisticas/**` y `/api/resumen` → `lb://servicio-estadisticas`
   - Con el login encendido, los 4 servicios ya validan el token por su cuenta; el gateway debe **reenviar** el encabezado `Authorization` sin tocarlo (validarlo también en el gateway es opcional).
3. **CORS Global:**
   - Habilitar CORS en el Gateway para permitir peticiones desde `http://localhost:4200` (Frontend) con métodos `GET, POST, PUT, DELETE, OPTIONS`.
4. **Dockerización:**
   - Incluir `Dockerfile` y agregar el servicio en el `docker-compose.yml` de la raíz en puerto `8000`.

---

##  Estado Actual
Este módulo está **pendiente**: todavía no existe código. Los 4 microservicios ya tienen sus contratos REST, se registran en Eureka y validan el token; el frontend Angular (`:4200`) hoy usa un proxy de desarrollo (`frontend/proxy.conf.json`) que el gateway vendrá a reemplazar.
