# HelpDesk

Plataforma de tickets de soporte construida como **microservicios** independientes.
Primero el backend (Spring Boot); el frontend vendra despues.

> **Estado: fase 1, servicios simples.** Cada servicio es lo mas sencillo posible, igual que
> `espacios-service`: sus entidades (`model/`), un servicio con la logica (`service/`) y un controller
> (`controller/`), con los datos **en memoria**, para probarlo con Postman. No hay base de datos ni
> capas adicionales todavia; se agregan cuando
> hagan falta (ver [Arquitectura de cada servicio](#arquitectura-de-cada-servicio)). Lo marcado como
> *(planificado)* se ira construyendo por fases (ver [Hoja de ruta](#hoja-de-ruta)).

## Tabla de contenido

- [Stack](#stack)
- [Arquitectura general](#arquitectura-general)
- [Arquitectura de cada servicio](#arquitectura-de-cada-servicio)
- [Organizacion de carpetas](#organizacion-de-carpetas)
- [Probar con Postman](#probar-con-postman)
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
  +-----------+ +-----------+ +--------------+ +---------------+
  |  servicio | |  servicio | |   servicio   | |    servicio   |
  |  tickets  | | identidad | | estadisticas | | comunicacion  |
  +-----+-----+ +-----+-----+ +------+-------+ +-------+-------+
        |             |              |                 |
        v             v              v                 v
   tickets_db   identidad_db   (solo lectura)   comunicacion_db
```

### Servicios

| Servicio | Responsabilidad | Puerto local | Estado | Responsable |
|---|---|---|---|---|
| `servicio-tickets` | Nucleo: crear tickets, prioridad, asignacion, estados (falta: categoria e historial de cambios) | 8080 | Entidades + endpoints basicos (en memoria) | por asignar |
| `servicio-estadisticas` | Estadisticas por estado (falta: categoria, prioridad, tiempo promedio, carga de tecnicos) | 8082 | Entidades + endpoints basicos (en memoria) | por asignar |
| `servicio-identidad` | Usuarios y roles (falta: inicio de sesion y permisos) | 8083 | Entidades + endpoints basicos (en memoria) | por asignar |
| `servicio-comunicacion` | Comentarios y notificaciones de cambio de estado, avisos pendientes o leidos | 8084 | Entidades + endpoints basicos (en memoria) | por asignar |

Quien es dueno de que dato (para no pisarse):

- **tickets** guarda el `id` del usuario, nunca una copia de sus datos.
- **comunicacion** guarda cada comentario con el `ticketId`; no toca la tabla de tickets.
- **estadisticas** no lee la base de datos de otro servicio: consulta la API de tickets o, mas adelante, recibe eventos.
- **identidad** es el unico que conoce credenciales y roles.

### Entidades y contrato entre servicios

Entidades minimas, una clase de dominio por concepto (en `domain/model` de cada servicio):

| Servicio | Entidad | Campos |
|---|---|---|
| identidad | `Usuario` | `id`, `nombre`, `rol` (`ESTUDIANTE`, `TECNICO`, `ADMINISTRADOR`) |
| tickets | `Ticket` | `id`, `titulo`, `descripcion`, `prioridad`, `estado`, `solicitanteId`, `tecnicoId` |
| comunicacion | `Comentario` | `id`, `ticketId`, `autorId`, `texto` |
| comunicacion | `Notificacion` | `id`, `usuarioId`, `mensaje`, `leida` |
| estadisticas | `ResumenTickets` | `abiertos`, `enProgreso`, `resueltos` (se calcula, no se guarda) |

Estados del ticket: `ABIERTO` -> `EN_PROGRESO` (al asignarlo) -> `RESUELTO`.

**Reglas para que se entiendan sin depender unos de otros:**

1. **Referencias solo por id.** Se guarda `ticketId`, `usuarioId`, `tecnicoId`; nunca una copia del dato ajeno.
2. **Cada servicio tiene su propia clase** para lo que lee de otro. Nada de librerias de dominio compartidas.
3. **Los estados viajan como texto** (`"RESUELTO"`). Quien lee ignora lo que no conoce, asi agregar un
   estado no rompe a los demas.
4. **El camino principal no llama a otros servicios.** Crear un ticket o un comentario solo usa la base
   de datos propia: si otro servicio cae, este sigue trabajando.
5. **Errores en formato `ProblemDetail`** (404, 400, 409) en todos los servicios.

**Evento oficial:** cuando un ticket cambia de estado, servicio-tickets lo anuncia con este formato.

```json
{ "ticketId": 7, "estadoAnterior": "ABIERTO", "estadoNuevo": "RESUELTO",
  "solicitanteId": 3, "fecha": "2026-10-24T10:15:00Z" }
```

`servicio-comunicacion` lo usa para avisar al solicitante y `servicio-estadisticas` para sus contadores;
cada uno lo lee con su propia clase `TicketEstadoCambiado` y lo recibe en `POST /eventos/ticket-estado`.
**`servicio-tickets` todavia no lo envia solo**: por ahora lo simulas tu desde Postman (ver
[Probar con Postman](#probar-con-postman)). Falta decidir como se envia de verdad (broker o llamada HTTP
con tiempo limite).

Principios:

1. **Un servicio por contexto de negocio** (tickets, identidad, analiticas, comunicacion).
2. **Base de datos propia por servicio.** Nadie lee la tabla de otro servicio; se pide por API.
3. **Sin codigo de dominio compartido.** Compartirlo acopla los servicios y obliga a desplegarlos juntos.
4. **Sin gateway por ahora.** Se agregara cuando haya mas de un servicio expuesto; la estructura ya le deja sitio.

## Arquitectura de cada servicio

Cada servicio es lo mas simple posible, igual que `espacios-service`: sin capas extra.

```
com.helpdesk.tickets
├─ ServicioTicketsApplication.java    Arranque de Spring Boot
├─ model/                            Entidades: Ticket, Prioridad, EstadoTicket
├─ service/                          TicketService: la logica y los datos en memoria
└─ controller/                       TicketController: recibe la peticion y llama al servicio
```

- **`model/`:** las entidades. Son clases simples (campos, constructor vacio y getters/setters) para que
  Spring convierta el JSON de Postman en objetos sin configuracion.
- **`service/`:** la logica. Valida lo basico y guarda los datos en memoria (se pierden al reiniciar),
  respondiendo 400 / 404 / 409 con el motivo del error.
- **`controller/`:** los endpoints. No tiene logica, solo traduce HTTP al servicio.
- **Entre servicios:** referencias solo por id y ninguna llamada a otro servicio al crear algo, para que si
  uno cae los demas sigan trabajando.

**Por que tan simple:** el objetivo ahora es que todo el equipo pueda levantar el servicio y probarlo con
Postman. Cuando un servicio crezca (base de datos, reglas de negocio, muchos endpoints) se separa en capas,
por ejemplo `domain` / `application` / `infrastructure`, sin cambiar la API ni a los demas servicios
([ADR 0002](docs/adr/0002-servicios-simples.md)).

## Organizacion de carpetas

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
└─ services/                      Un subdirectorio por microservicio (cada uno independiente)
   ├─ servicio-tickets/           Puerto 8080 (tiene ademas Dockerfile propio)
   ├─ servicio-identidad/         Puerto 8083
   ├─ servicio-comunicacion/      Puerto 8084
   └─ servicio-estadisticas/      Puerto 8082
```

Dentro de cada servicio:

```
<servicio>/
├─ pom.xml                        Proyecto Maven propio
└─ src/
   ├─ main/java/com/helpdesk/<nombre>/
   │  ├─ Servicio<Nombre>Application.java
   │  ├─ model/                   Entidades
   │  ├─ service/                 Logica
   │  └─ controller/              Endpoints
   ├─ main/resources/application.properties    Nombre y puerto del servicio
   └─ test/java/                  Tests del servicio (sin levantar Spring)
```

Se agregara en las siguientes fases *(planificado)*: POM padre, `docker-compose.yml` con PostgreSQL,
`.env.example`, `scripts/` y los workflows de CI (`ci.yml`, `docker.yml`).

Convenciones de nombres:

- Carpetas y archivos en **minusculas con guiones**, **sin tildes ni enie** (`estadisticas`, no `estadísticas`):
  las tildes dan problemas de codificacion entre Windows, macOS y Linux.
- Servicios en espanol, nombrados `servicio-<dominio>` (`servicio-tickets`, `servicio-identidad`...).
- Paquete de Java: `com.helpdesk.<dominio>` (`com.helpdesk.identidad`); el nombre de la carpeta debe coincidir
  con la linea `package` de cada clase.
- Clase de arranque: `Servicio<Dominio>Application` (el sufijo `Application` es la convencion de Spring Boot).

## Probar con Postman

Sin instalar Java ni Maven, solo con Docker. Desde la carpeta del servicio (por ejemplo
`services/servicio-identidad`), con el puerto que le corresponde:

```bash
docker run --rm -v "$PWD:/src" -w /src -p 8083:8083 maven:3.9-eclipse-temurin-21-alpine mvn -q -B spring-boot:run
```

Para correr los tests: `docker run --rm -v "$PWD:/src" -w /src maven:3.9-eclipse-temurin-21-alpine mvn -B test`.
Los datos viven en memoria: al reiniciar el servicio empiezan de cero.

| Servicio | Puerto | Metodo y ruta | Que hace |
|---|---|---|---|
| tickets | 8080 | `POST /tickets` | Crea un ticket (nace `ABIERTO`) |
| | | `GET /tickets`, `GET /tickets/{id}` | Lista / consulta |
| | | `PUT /tickets/{id}/asignar/{tecnicoId}` | Asigna un tecnico y pasa a `EN_PROGRESO` |
| | | `PUT /tickets/{id}/resolver` | Pasa a `RESUELTO` |
| identidad | 8083 | `POST /usuarios`, `GET /usuarios`, `GET /usuarios/{id}` | Crea y consulta usuarios |
| comunicacion | 8084 | `POST /comentarios`, `GET /comentarios?ticketId=1` | Comentarios de un ticket |
| | | `GET /notificaciones?usuarioId=3&leida=false` | Avisos de un usuario |
| | | `PUT /notificaciones/{id}/leida` | Marca un aviso como leido |
| | | `POST /eventos/ticket-estado` | Recibe el evento y crea el aviso |
| estadisticas | 8082 | `GET /resumen` | Abiertos / en progreso / resueltos |
| | | `POST /eventos/ticket-estado` | Recibe el evento y actualiza los contadores |

Cuerpos JSON de ejemplo (en Postman: *Body > raw > JSON*):

```jsonc
// POST localhost:8080/tickets
{ "titulo": "Wifi caido", "descripcion": "Sala 2", "prioridad": "ALTA", "solicitanteId": 3 }

// POST localhost:8083/usuarios            (rol: ESTUDIANTE | TECNICO | ADMINISTRADOR)
{ "nombre": "Ana", "rol": "ESTUDIANTE" }

// POST localhost:8084/comentarios
{ "ticketId": 1, "autorId": 9, "texto": "Ya lo revise" }

// POST localhost:8084/eventos/ticket-estado   y   POST localhost:8082/eventos/ticket-estado
{ "ticketId": 1, "estadoAnterior": "EN_PROGRESO", "estadoNuevo": "RESUELTO", "solicitanteId": 3 }
```

Para ver la comunicacion entre servicios: crea un ticket, asignalo y resuelvelo en tickets; luego envia el
mismo evento a comunicacion (aparece un aviso para el solicitante) y a estadisticas (cambia el resumen).

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
- [x] **Fase 1, servicios simples:** los cuatro servicios con sus entidades y endpoints basicos en memoria, probables con Postman.
- [ ] **Fase 1b, infraestructura comun:** POM padre, Compose con PostgreSQL.
- [ ] **Fase 2, CI:** workflow de build y tests por ruta; ejecutarlo en local con [`act`](https://github.com/nektos/act).
- [ ] **Fase 3, desarrollar los servicios:** cada responsable completa el suyo (campos que faltan, base de datos, y capas si el servicio crece).
- [ ] **Fase 4, gateway y seguridad:** API Gateway, autenticacion JWT.
- [ ] **Fase 5, frontend.**
