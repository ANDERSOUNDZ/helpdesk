# 📘 ARQUITECTURA MAESTRA Y REGLAS DE ORO — HELPDESK

> **DOCUMENTO OFICIAL DEL PROYECTO (GUÍA ÚNICA)**  
> Este documento unifica toda la arquitectura técnica, las reglas de desarrollo, el funcionamiento del orquestador, los contratos de datos y la planificación del API Gateway y Frontend. Todo compañero de equipo debe regirse por este estándar.

---

## 🎯 PROPÓSITO GENERAL Y ENFOQUE FUNCIONAL DE TI

El sistema **HelpDesk** es una **Plataforma Distribuida de Mesa de Ayuda de TI (IT Service Desk)** cuyo objetivo primordial es **centralizar, canalizar y resolver incidentes y requerimientos tecnológicos** de una organización (fallos de red, accesos, equipos o software).

### 👥 Asignación Oficial de Desarrolladores por Componente:
| Desarrollador | Componente / Responsabilidad | Carpeta | Puerto App | Puerto BD | pgAdmin |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Ariel Martín Guerrero Sandoval** | **Servicio de Tickets** (Núcleo de incidentes) | `services/servicio-tickets` | `8080` | `5433` | `5050` |
| **Gabriel Emilio Ponce Salazar** | **Servicio de Comunicación** (Comentarios y avisos) | `services/servicio-comunicacion` | `8084` | `5435` | `5054` |
| **Miguel Anderson Chanchay** | **Servicio de Estadísticas** (Métricas y auditoría TI) | `services/servicio-estadisticas` | `8082` | `5436` | `5052` |
| **Dylan Miguel Vargas Onofa** | **Frontend SPA + API Gateway** (Entrada y UI Web) | `frontend` / `gateway` | `3000` / `8000` | N/A | N/A |
| *Módulo Base Compartido* | **Servicio de Identidad** (Usuarios y roles) | `services/servicio-identidad` | `8083` | `5434` | `5053` |

---
1. **Detección y Apertura:** Un empleado (`USUARIO`) detecta una anomalía y registra un ticket en el sistema con un nivel de prioridad asignado.
2. **Triaje y Asignación:** El soporte técnico o administrador asigna el ticket a un especialista (`TECNICO`), moviendo el estado a `EN_PROGRESO`.
3. **Diagnóstico y Retroalimentación:** El técnico agrega notas técnicas o solicita más datos mediante el módulo de comunicación; el usuario recibe notificaciones de cada avance.
4. **Cierre y Solución:** El técnico marca el ticket como `RESUELTO`.
5. **Métricas de Calidad:** Se recalculan los contadores en tiempo real para evaluar tiempos de respuesta y carga de trabajo en el departamento de TI.

---

## 1. 🛑 LAS 5 REGLAS DE ORO INMUTABLES (OBLIGATORIAS)

Para que cualquier desarrollador pueda clonar el proyecto, agregar código y probar en su máquina sin que los microservicios de los demás colapsen, existen 5 reglas obligatorias:

1. **PROHIBIDO compartir Entidades (`@Entity`) entre servicios:**  
   Cada microservicio es dueño absoluto de sus tablas. Para vincular información de otro servicio (usuario, ticket, autor), usa **únicamente IDs numéricos primitivos** (`Long solicitanteId`, `Long ticketId`, `Long autorId`).  
   ❌ *Prohibido:* `@ManyToOne private Usuario usuario;` (acopla las bases de datos y tumba el proyecto si el compañero cambia su tabla).  
   ✅ *Correcto:* `private Long solicitanteId;` (desacoplado y 100% seguro).

2. **PROHIBIDO llamadas HTTP bloqueantes directas entre microservicios:**  
   Ningún microservicio llama por `RestTemplate` o `WebClient` directo a otro al crear, editar o cambiar estados. Si el servicio de tu compañero está apagado en local, tu código fallaría con error 500. La sincronía se hace mediante el **Bus de Eventos (Asíncrono)** o a través del **API Gateway**.

3. **PROHIBIDO alterar los puertos y nombres de base de datos asignados:**  
   Son contratos estáticos de red que deben mantenerse idénticos:
   - `servicio-tickets`: App **`8080`** | PostgreSQL **`5433`** (`tickets_db`)
   - `servicio-estadisticas`: App **`8082`** | PostgreSQL **`5436`** (`estadisticas_db`)
   - `servicio-identidad`: App **`8083`** | PostgreSQL **`5434`** (`identidad_db`)
   - `servicio-comunicacion`: App **`8084`** | PostgreSQL **`5435`** (`comunicacion_db`)
   - *API Gateway (Futuro):* Puerto **`8000`** | *Frontend Web (Futuro):* Puerto **`3000`**

4. **PROTOCOLO DE IDENTIFICACIÓN, ASIGNACIÓN HERMÉTICA Y PRIVILEGIOS DE DUEÑO:**  
   - 👑 **Miguel Anderson Chanchay** (`@ANDERSOUNDZ` / `miguelchanchay`) es el **Dueño y Administrador del Repositorio**. Tiene **acceso irrestricto y total** a todos los microservicios, infraestructura, Docker y ramas para resolver bloqueos, administrar arquitectura y aprobar PRs.
   - Para cualquier otro desarrollador, el agente verificará: **nombre completo y usuario de GitHub conectado**.
   - Cada colaborador solo puede modificar su carpeta hermética y su rama:
     - **Ariel Martín Guerrero Sandoval** ➔ Únicamente en `services/servicio-tickets/` (`feat/tickets`).
     - **Gabriel Emilio Ponce Salazar** ➔ Únicamente en `services/servicio-comunicacion/` (`feat/comunicacion`).
     - **Dylan Miguel Vargas Onofa** ➔ Únicamente en `frontend/` y `gateway/` (`feat/frontend`, `feat/gateway`).  
   *Regla inquebrantable:* Ningún colaborador puede tocar código ajeno ni hacer push directo a `dev` ni a `main`. Todo cambio requiere Pull Request firmado por el colaborador asignado y aprobado por Miguel Chanchay.

5. **NUNCA ejecutar comandos destructivos de Docker:**  
   ❌ `docker compose down -v` (Borra los volúmenes y las bases de datos).  
   ✅ `docker compose down` (Detiene los contenedores y **conserva intactos** los datos).

---

## 2. 🗺️ ROADMAP DEL PROYECTO & ESTADO DE LOS PROCESOS

```mermaid
flowchart LR
    F0["Fase 0\nFundación\n(Monorepo & Git)"]:::done --> F1["Fase 1\nMicroservicios Base\n(4 Servicios + Postgres Indep.)"]:::done
    F1 --> F2["Fase 2\nOrquestación & Eventos\n(Bus RabbitMQ + Contratos)"]:::next
    F2 --> F3["Fase 3\nAPI Gateway\n(Puerto :8000)"]:::pending
    F3 --> F4["Fase 4\nFrontend Web\n(Puerto :3000)"]:::pending

    classDef done fill:#2ea44f,stroke:#22863a,stroke-width:2px,color:#fff;
    classDef next fill:#f97515,stroke:#d15700,stroke-width:2px,color:#fff;
    classDef pending fill:#6a737d,stroke:#586069,stroke-width:2px,color:#fff,stroke-dasharray: 4 4;
```

### Estado de cada Fase:
- [x] **Fase 0 (Completada):** Estructura monorepo, convenciones Git por integrante (`feat/<servicio>` ➔ `dev` ➔ `main`) y CI de cherry-pick.
- [x] **Fase 1 (Completada y Operativa):** 4 microservicios funcionales con Spring Boot 4 y Spring Data JPA. Bases de datos PostgreSQL independientes por servicio y base general. 16 pruebas unitarias passing (100% éxito).
- [ ] **Fase 2 (Siguiente):** Bus de Eventos global (RabbitMQ) y módulo común de contratos (`helpdesk-common`).
- [ ] **Fase 3 (Especificada):** API Gateway en Spring Cloud Gateway (Puerto `8000`) para enrutamiento único y CORS centralizado.
- [ ] **Fase 4 (Especificada):** Frontend Web SPA (Puerto `3000`) con contratos TypeScript listos.

---

## 3. 🔄 EL ORQUESTADOR: MALLA DE EVENTOS GLOBAL ENTRE LOS 4 SERVICIOS

Para que todos los servicios sincronicen su información sin acoplarse, se utiliza un **Bus de Eventos Asíncrono (RabbitMQ)**. Cuando ocurre una acción importante en un microservicio, este publica un evento al bus y los demás servicios reaccionan automáticamente:

```mermaid
flowchart TD
    subgraph Microservicios ["Los 4 Microservicios Autónomos"]
        T["servicio-tickets\n(8080)"]
        I["servicio-identidad\n(8083)"]
        C["servicio-comunicacion\n(8084)"]
        E["servicio-estadisticas\n(8082)"]
    end

    BUS[("BUS GLOBAL DE EVENTOS\n(Event Broker / RabbitMQ)")]

    T -->|"1. Publica: TicketCreado, TicketEstadoCambiado, TicketAsignado"| BUS
    I -->|"2. Publica: UsuarioCreado, UsuarioDesactivado"| BUS
    C -->|"3. Publica: ComentarioAgregado"| BUS

    BUS -->|"Consume para enviar avisos/notificaciones"| C
    BUS -->|"Consume para actualizar contadores y métricas"| E
    BUS -->|"Consume para liberar tickets si se elimina un técnico"| T
```

### Matriz de Eventos Global:
| Evento | Emite | Reacciona / Consume | Efecto en el Sistema |
| :--- | :---: | :---: | :--- |
| **`TicketCreadoEvent`** | `tickets` | `comunicacion`, `estadisticas` | Envía notificación al solicitante y suma +1 al contador de tickets abiertos. |
| **`TicketEstadoCambiadoEvent`** | `tickets` | `comunicacion`, `estadisticas` | Notifica al usuario (*"Tu ticket se resolvió"*) y actualiza las gráficas en tiempo real. |
| **`TicketAsignadoEvent`** | `tickets` | `comunicacion` | Notifica al técnico asignado con su nueva tarea. |
| **`UsuarioCreadoEvent`** | `identidad` | `comunicacion`, `estadisticas` | Inicializa la bandeja del usuario y actualiza estadísticas por rol. |
| **`UsuarioDesactivadoEvent`** | `identidad` | `tickets`, `comunicacion` | Si se elimina un técnico, `tickets` libera sus tickets para reasignarlos. |
| **`ComentarioAgregadoEvent`** | `comunicacion` | `tickets`, `estadisticas` | Actualiza la última actividad del ticket y calcula tiempos de respuesta. |

---

## 4. 📦 ESTANDARIZACIÓN DE DATOS (MÓDULO `helpdesk-common`)

Para no duplicar clases ni compartir entidades de base de datos, se creará un módulo Java ligero con clases planas (`records` de Java, sin JPA):

```java
// Contrato de evento para el Bus
public record TicketEstadoCambiadoEvent(
    Long ticketId,
    String titulo,
    String estadoAnterior,
    String estadoNuevo,
    Long solicitanteId
) {}

// DTO estándar para exponer datos entre servicios o al Frontend
public record UsuarioResumenDTO(
    Long id,
    String nombre,
    String rol
) {}
```

---

## 5. 🌐 ESPECIFICACIÓN: API GATEWAY Y FRONTEND (DYLAN MIGUEL VARGAS ONOFA)

> **Desarrollador Responsable:** **Dylan Miguel Vargas Onofa**  
> **Ramas asignadas:** `feat/gateway`, `feat/frontend`  
> **Puertos:** Gateway (`8000`) | Frontend Web SPA (`3000`)

### 👨‍💻 Para el Desarrollador (Dylan Miguel Vargas Onofa)
#### ¿Qué está hecho hasta ahora?
- Los 4 microservicios backend ya cuentan con endpoints REST funcionales probados en sus puertos individuales (`:8080`, `:8083`, `:8084`, `:8082`).
- Las entidades base (`Ticket`, `Usuario`, `Comentario`, `Notificacion`, `ContadorEstado`) ya están definidas y probadas con pruebas unitarias.

#### ¿Qué debes revisar y qué sigue en tu roadmap?
1. **API Gateway (Spring Cloud Gateway en `:8000`):**
   - Centralizar todas las peticiones para que el frontend no tenga que conocer 4 puertos distintos.
   - Habilitar CORS globalmente para permitir peticiones desde `http://localhost:3000`.
   - Mapear las rutas limpias:
     - `/api/tickets/**` ➔ `servicio-tickets:8080/tickets/**`
     - `/api/usuarios/**` ➔ `servicio-identidad:8083/usuarios/**`
     - `/api/comentarios/**` ➔ `servicio-comunicacion:8084/comentarios/**`
     - `/api/notificaciones/**` ➔ `servicio-comunicacion:8084/notificaciones/**`
     - `/api/estadisticas/**` ➔ `servicio-estadisticas:8082/resumen`
2. **Frontend SPA (React / Vue / Angular en `:3000`):**
   - **Vista de Tickets (Ariel):** Lista de tickets, filtro por estado (`ABIERTO`, `EN_PROGRESO`, `RESUELTO`), formulario de creación y botón de asignación.
   - **Panel de Comunicación (Gabriel):** Sección de notas/comentarios dentro del detalle de un ticket y campana de notificaciones del usuario.
   - **Dashboard de Métricas (Miguel):** Tarjetas con contadores de tickets abiertos vs resueltos consumiendo `/api/estadisticas`.

---

### A. API Gateway (Puerto `8000`)
Punto de entrada único para el Frontend. Enruta de forma transparente:
- `/api/tickets/**` ➔ `http://servicio-tickets:8080/tickets/**`
- `/api/usuarios/**` ➔ `http://servicio-identidad:8083/usuarios/**`
- `/api/comentarios/**` ➔ `http://servicio-comunicacion:8084/comentarios/**`
- `/api/notificaciones/**` ➔ `http://servicio-comunicacion:8084/notificaciones/**`
- `/api/estadisticas/**` ➔ `http://servicio-estadisticas:8082/resumen`

### B. Contratos TypeScript para el Frontend
El Frontend se conecta únicamente a `http://localhost:8000/api` usando estos contratos:
```typescript
export type Prioridad = 'BAJA' | 'MEDIA' | 'ALTA' | 'URGENTE';
export type EstadoTicket = 'ABIERTO' | 'EN_PROCESO' | 'RESUELTO' | 'CERRADO';

export interface Ticket {
  id: number;
  titulo: string;
  descripcion?: string;
  prioridad: Prioridad;
  estado: EstadoTicket;
  solicitanteId: number;
  tecnicoId?: number | null;
}

export interface Usuario {
  id: number;
  nombre: string;
  rol: 'ESTUDIANTE' | 'DOCENTE' | 'TECNICO' | 'ADMIN';
}

export interface Comentario {
  id: number;
  ticketId: number;
  autorId: number;
  contenido: string;
}

export interface ResumenEstadisticas {
  abiertos: number;
  enProceso: number;
  resueltos: number;
  cerrados: number;
  total: number;
}
```

---

## 6. 💻 GUÍA PRÁCTICA DE EJECUCIÓN (CÓMO TRABAJAR)

### Modo 1: Trabajar en TU servicio (Modo Aislado - Día a Día)
No necesitas levantar los servicios de tus compañeros. Solo abre Docker Desktop:
```powershell
cd services/servicio-tickets
docker compose up -d --build
```
- Tu app correrá en: `http://localhost:8080`
- Tu base PostgreSQL correrá en: `localhost:5433`
- Para apagar sin perder datos: `docker compose down`

### Modo 2: Levantar TODO el sistema integrado
Desde la raíz del proyecto para demostraciones o pruebas de integración:
```powershell
cd HelpDesk
docker compose up -d --build
```
- Levanta una sola base PostgreSQL (puerto `5432`) con las 4 bases de datos inicializadas y los 4 microservicios en simultáneo.
