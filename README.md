# HelpDesk

Plataforma de tickets de soporte construida como **microservicios** con **arquitectura Clean**.
Primero el backend (Spring Boot); el frontend vendra despues.

> **Estado: fase 1, servicios iniciales.** El repositorio define la estructura, las reglas de equipo
> y el flujo de trabajo. Hay un servicio con funcionalidad (`tickets-service`) y tres con solo
> el esqueleto de capas, listos para que cada persona los desarrolle. Lo marcado como
> *(planificado)* se ira construyendo por fases (ver [Hoja de ruta](#hoja-de-ruta)).

## Tabla de contenido

- [Stack](#stack)
- [Arquitectura general](#arquitectura-general)
- [Arquitectura de cada servicio](#arquitectura-de-cada-servicio)
- [Organizacion de carpetas](#organizacion-de-carpetas)
- [Datos y comunicacion entre servicios](#datos-y-comunicacion-entre-servicios)
- [Entorno de desarrollo (Windows, macOS, Linux)](#entorno-de-desarrollo-windows-macos-linux)
- [Flujo de trabajo del equipo](#flujo-de-trabajo-del-equipo)
- [Hoja de ruta](#hoja-de-ruta)

## Stack

| Capa | Tecnologia | Notas |
|---|---|---|
| Lenguaje | Java 21 | Igual que `espacios-service` |
| Framework | Spring Boot 4.x | Misma base que el proyecto de referencia |
| Build | Maven (multi-modulo) | POM padre en la raiz *(planificado)* |
| Base de datos | PostgreSQL + Flyway | Una base por servicio *(planificado)* |
| Contenedores | Docker + Docker Compose | Entorno unico para el equipo |
| CI | GitHub Actions | Ejecutable en local con `act` *(planificado)* |

## Arquitectura general

Cada servicio es **independiente**: su propio codigo, su propia base de datos, su propia imagen
Docker y su propio ciclo de despliegue. Viven juntos en un monorepo solo para facilitar la
coordinacion del equipo ([ADR 0001](docs/adr/0001-monorepo-clean-microservicios.md)).

```
            Clientes (frontend, mas adelante)
                         |
                         v
   +-----------------------------------------------+
   |  API Gateway   (fuera de alcance por ahora)   |
   +-----------------------------------------------+
        |            |            |            |
        v            v            v            v
  +-----------+ +-----------+ +-----------+ +---------------+
  |  tickets  | | identity  | | analytics | | communication |
  |  service  | |  service  | |  service  | |    service    |
  +-----+-----+ +-----+-----+ +-----+-----+ +-------+-------+
        |             |             |                |
        v             v             v                v
   tickets_db    identity_db   (solo lectura)   communication_db
```

### Servicios

| Servicio | Responsabilidad | Puerto local | Estado | Responsable |
|---|---|---|---|---|
| `tickets-service` | Nucleo: crear tickets, prioridad, categoria, asignacion, estados e historial de cambios | 8080 | Funcional (en memoria) | por asignar |
| `analytics-service` | Estadisticas: tickets por estado, categoria y prioridad, tiempo promedio de resolucion, carga de tecnicos | 8082 | Solo esqueleto | por asignar |
| `identity-service` | Usuarios, inicio de sesion, roles (estudiante, tecnico, administrador) y permisos | 8083 | Solo esqueleto | por asignar |
| `communication-service` | Comentarios y respuestas, notificaciones de cambio de estado, avisos pendientes o leidos | 8084 | Solo esqueleto | por asignar |

Quien es dueno de que dato (para no pisarse):

- **tickets** guarda el `id` del usuario, nunca una copia de sus datos.
- **communication** guarda cada comentario con el `ticketId`; no toca la tabla de tickets.
- **analytics** no lee la base de datos de otro servicio: consulta la API de tickets o, mas adelante, recibe eventos.
- **identity** es el unico que conoce credenciales y roles.

### Entidades y contrato entre servicios

Entidades minimas, una clase de dominio por concepto (en `domain/model` de cada servicio):

| Servicio | Entidad | Campos |
|---|---|---|
| identity | `Usuario` | `id`, `nombre`, `rol` (`ESTUDIANTE`, `TECNICO`, `ADMINISTRADOR`) |
| tickets | `Ticket` | `id`, `titulo`, `descripcion`, `prioridad`, `estado`, `solicitanteId`, `tecnicoId` |
| communication | `Comentario` | `id`, `ticketId`, `autorId`, `texto` |
| communication | `Notificacion` | `id`, `usuarioId`, `mensaje`, `leida` |
| analytics | `ResumenTickets` | `abiertos`, `enProgreso`, `resueltos` (se calcula, no se guarda) |

Estados del ticket: `ABIERTO` -> `EN_PROGRESO` (al asignarlo) -> `RESUELTO`.

**Reglas para que se entiendan sin depender unos de otros:**

1. **Referencias solo por id.** Se guarda `ticketId`, `usuarioId`, `tecnicoId`; nunca una copia del dato ajeno.
2. **Cada servicio tiene su propia clase** para lo que lee de otro. Nada de librerias de dominio compartidas.
3. **Los estados viajan como texto** (`"RESUELTO"`). Quien lee ignora lo que no conoce, asi agregar un
   estado no rompe a los demas.
4. **El camino principal no llama a otros servicios.** Crear un ticket o un comentario solo usa la base
   de datos propia: si otro servicio cae, este sigue trabajando.
5. **Errores en formato `ProblemDetail`** (404, 400, 409) en todos los servicios.

**Evento oficial:** cuando un ticket cambia de estado, tickets-service lo anuncia con este formato.

```json
{ "ticketId": 7, "estadoAnterior": "ABIERTO", "estadoNuevo": "RESUELTO",
  "solicitanteId": 3, "fecha": "2026-10-24T10:15:00Z" }
```

`communication-service` lo usa para avisar al solicitante y `analytics-service` para sus contadores;
cada uno lo lee con su propia clase `TicketEstadoCambiado`. **Hoy el evento solo esta definido**: aun no
se publica ni se recibe (falta decidir broker o llamada HTTP con tiempo limite).

Principios:

1. **Un servicio por contexto de negocio** (tickets, identidad, analiticas, comunicacion).
2. **Base de datos propia por servicio.** Nadie lee la tabla de otro servicio; se pide por API.
3. **Sin codigo de dominio compartido.** Compartirlo acopla los servicios y obliga a desplegarlos juntos.
4. **Sin gateway por ahora.** Se agregara cuando haya mas de un servicio expuesto; la estructura ya le deja sitio.

## Arquitectura de cada servicio

Todos los servicios siguen la misma estructura **Clean / hexagonal**. La idea: la logica de negocio
no sabe nada de Spring, de la base de datos ni de HTTP, asi que se puede probar y cambiar sin romper el resto.

```
        +-----------------------------------------------------+
        |  api               (controllers, DTOs, errores HTTP)|
        |  infrastructure    (JPA, adapters, configuracion)   |
        |        |                                            |
        |        v   las dependencias apuntan hacia adentro   |
        |  +---------------------------------------------+    |
        |  |  application   (casos de uso, puertos)      |    |
        |  |       |                                     |    |
        |  |       v                                     |    |
        |  |  +-------------------------------------+    |    |
        |  |  |  domain  (entidades y reglas puras) |    |    |
        |  |  +-------------------------------------+    |    |
        |  +---------------------------------------------+    |
        +-----------------------------------------------------+
```

| Capa | Contiene | Puede depender de |
|---|---|---|
| `domain` | Entidades, objetos de valor, reglas de negocio, excepciones de dominio | Nada (Java puro) |
| `application` | Casos de uso y **puertos**: interfaces `in` (lo que ofrece) y `out` (lo que necesita) | `domain` |
| `infrastructure` | Implementa los puertos `out`: entidades JPA, repositorios, mappers, configuracion | `application`, `domain`, Spring |
| `api` | Controllers REST, DTOs de entrada y salida, manejo de errores | `application`, Spring Web |

Reglas que no se rompen:

- `domain` **no importa** `org.springframework.*` ni `jakarta.persistence.*`.
- La entidad JPA y la entidad de dominio son **clases distintas**; un mapper las conecta.
  (En `espacios-service` hoy son la misma clase y por eso el modelo queda atado a la base de datos.)
- Los controllers **no devuelven** entidades, devuelven DTOs.
- Se hara cumplir con un test de **ArchUnit** que falla si alguien viola las dependencias *(planificado)*.

Estructura de paquetes de un servicio:

```
com.helpdesk.tickets
├─ domain/
│  ├─ model/                 Ticket, Prioridad, EstadoTicket
│  └─ exception/             TicketNoEncontradoException
├─ application/
│  ├─ port/in/               CrearTicketUseCase, ObtenerTicketUseCase
│  ├─ port/out/              TicketRepositoryPort
│  └─ service/               CrearTicketService (implementa los casos de uso)
├─ infrastructure/
│  ├─ persistence/           TicketJpaEntity, TicketJpaRepository, TicketPersistenceAdapter, TicketMapper
│  └─ config/                BeanConfig
└─ api/
   ├─ controller/            TicketController
   ├─ dto/                   CrearTicketRequest, TicketResponse
   └─ error/                 GlobalExceptionHandler
```

## Organizacion de carpetas

Lo que existe hoy y lo que viene:

```
helpdesk/
├─ README.md                      Este documento
├─ CONTRIBUTING.md                Ramas, commits, PR, cherry-pick, multiplataforma
├─ .gitignore                     Ignora target/, .env, datos locales, IDEs
├─ .gitattributes                 Fuerza LF: mismo resultado en Windows, Mac y Linux
├─ .editorconfig                  Indentacion y charset iguales en cualquier IDE
├─ .github/
│  ├─ CODEOWNERS                  Quien revisa cada carpeta
│  ├─ pull_request_template.md    Plantilla de PR
│  └─ workflows/
│     └─ cherry-pick.yml          Cherry-pick automatizado hacia otra rama
├─ docs/
│  └─ adr/                        Decisiones de arquitectura (ADR)
│     └─ 0001-monorepo-clean-microservicios.md
└─ services/                      Un subdirectorio por microservicio
   ├─ tickets-service/            Servicio con funcionalidad: referencia para los demas
   │  ├─ pom.xml                  Proyecto Maven propio (independiente de los demas)
   │  ├─ Dockerfile               Imagen propia; se construye y corre sin Java local
   │  ├─ .dockerignore
   │  └─ src/
   │     ├─ main/java/com/helpdesk/tickets/    Capas domain / application / infrastructure / api
   │     ├─ main/resources/application.properties
   │     └─ test/java/            Tests de dominio, de caso de uso y de arranque de Spring
   ├─ analytics-service/          Solo esqueleto (misma forma que identity-service)
   ├─ communication-service/      Solo esqueleto (misma forma que identity-service)
   └─ identity-service/           Solo esqueleto
      ├─ pom.xml
      └─ src/main/
         ├─ java/com/helpdesk/identity/
         │  ├─ IdentityServiceApplication.java
         │  ├─ domain/            package-info.java: que va en la capa y por que
         │  ├─ application/       package-info.java
         │  └─ infrastructure/    package-info.java (los controllers iran en infrastructure/web)
         └─ resources/application.properties
```

Los servicios nuevos arrancan con **tres capas** (`domain`, `application`, `infrastructure`) y nada mas.
Se usa `package-info.java` en cada capa porque Git no guarda carpetas vacias y asi, ademas,
queda escrito el proposito de la capa. Los controllers viven en `infrastructure/web`;
`tickets-service` conserva su capa `api/` y puede alinearse mas adelante.

Se agregara en las siguientes fases *(planificado)*:

```
helpdesk/
├─ pom.xml                        POM padre: versiones y dependencias comunes
├─ docker-compose.yml             Servicios + PostgreSQL
├─ .env.example                   Variables de entorno de ejemplo (sin secretos)
├─ scripts/                       Atajos en bash: build, test, up, ci-local
├─ .github/workflows/
│  ├─ ci.yml                      Build y tests por servicio, solo si cambio su carpeta
│  └─ docker.yml                  Construccion de imagenes
└─ services/tickets-service/
   └─ src/main/resources/db/migration/    Scripts de Flyway: V1__crear_tickets.sql
```

### tickets-service: como probarlo

Sin instalar Java ni Maven, solo con Docker (desde `services/tickets-service`):

```bash
docker run --rm -v "$PWD:/src" -w /src maven:3.9-eclipse-temurin-21-alpine mvn -B test   # tests
docker build -t tickets-service .                                                        # imagen
docker run --rm -p 8080:8080 tickets-service                                             # correr
```

| Metodo | Ruta | Que hace |
|---|---|---|
| `POST` | `/tickets` | Crea un ticket `{"titulo","descripcion","prioridad","solicitanteId"}` (`BAJA`/`MEDIA`/`ALTA`) |
| `GET` | `/tickets` | Lista los tickets |
| `GET` | `/tickets/{id}` | Obtiene un ticket |
| `PUT` | `/tickets/{id}/resolver` | Pasa el ticket a `RESUELTO` |

La asignacion a un tecnico ya existe en el dominio (`Ticket.asignarA`) pero aun no tiene endpoint.

Hoy los datos se guardan **en memoria** (se pierden al reiniciar) para fijar primero la
estructura; la base de datos se agrega despues sin tocar dominio ni aplicacion.

Convenciones: carpetas y archivos en **minusculas con guiones**; servicios nombrados `<dominio>-service`.

## Datos y comunicacion entre servicios

- **Base de datos:** PostgreSQL, **una base por servicio**, con migraciones Flyway versionadas.
  H2 queda solo para tests. (`ddl-auto=update`, usado hoy en `espacios-service`, no se usa mas alla del prototipo.)
- **Comunicacion:** REST sincrono al principio. Cuando haga falta desacoplar (por ejemplo,
  notificar al crear un ticket), se agregara mensajeria por eventos.
- **Salud:** cada servicio expone `/actuator/health`, usado por Docker Compose para saber si esta listo.

## Entorno de desarrollo (Windows, macOS, Linux)

El equipo usa los tres sistemas operativos. Para que el resultado sea identico en todos:

- **Docker es el entorno comun.** Se compila, prueba y ejecuta dentro de contenedores:
  no hace falta instalar Java ni Maven en tu maquina.
- `.gitattributes` fuerza fin de linea LF, y `.editorconfig` unifica el formato.
- Las imagenes base son multi-arquitectura, asi que funcionan en Apple Silicon (arm64) y en x86.
- Los scripts son `bash`; en Windows se corren con Git Bash o WSL2.

Requisitos y detalles por sistema: [CONTRIBUTING.md, seccion 5](CONTRIBUTING.md#5-trabajar-en-windows-macos-y-linux).

## Flujo de trabajo del equipo

- Dos ramas permanentes: `dev` (integracion diaria) y `main` (estable, solo recibe PR desde `dev`).
- Todo entra por **Pull Request** con 1 aprobacion y CI en verde *(cuando se active la proteccion de ramas)*.
- Una rama por tarea, que sale de `dev`: `feat/<servicio>/<descripcion>`. Cada persona es responsable de sus servicios (`CODEOWNERS`).
- Integracion con **squash merge** y commits [Conventional Commits](https://www.conventionalcommits.org/es/).
- **Cherry-pick** automatizado y manual solo para hotfixes y rescates puntuales:
  GitHub > Actions > *Cherry-pick a otra rama*.

Todo el detalle esta en [CONTRIBUTING.md](CONTRIBUTING.md).

## Hoja de ruta

- [x] **Fase 0, fundacion:** repositorio, estructura, reglas de equipo, cherry-pick automatizado.
- [x] **Fase 1, servicios iniciales:** `tickets-service` con Clean y Dockerfile; esqueleto de `identity`, `analytics` y `communication`.
- [ ] **Fase 1b, infraestructura comun:** POM padre, Compose con PostgreSQL.
- [ ] **Fase 2, CI:** workflow de build y tests por ruta; ejecutarlo en local con [`act`](https://github.com/nektos/act).
- [ ] **Fase 3, desarrollar los servicios:** cada responsable implementa el suyo (entidades, casos de uso, endpoints).
- [ ] **Fase 4, gateway y seguridad:** API Gateway, autenticacion JWT.
- [ ] **Fase 5, frontend.**
