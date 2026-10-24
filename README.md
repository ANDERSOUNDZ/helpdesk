# HelpDesk

Plataforma de tickets de soporte construida como **microservicios** independientes.
Primero el backend (Spring Boot); el frontend vendra despues.

> **Estado: fase 1, servicios simples con su propia base de datos.** Cada servicio es lo mas sencillo
> posible, igual que `espacios-service`: sus entidades (`model/`), su acceso a datos (`repository/`), un
> servicio con la logica (`service/`) y un controller (`controller/`). **Cada servicio tiene su propio
> PostgreSQL** y se puede levantar solo, con un comando, para trabajar de forma independiente
> (ver [Levantar el proyecto](#levantar-el-proyecto)). Las capas de Clean se agregan cuando hagan falta
> (ver [Arquitectura de cada servicio](#arquitectura-de-cada-servicio)). Lo marcado como
> *(planificado)* se ira construyendo por fases (ver [Hoja de ruta](#hoja-de-ruta)).

## Tabla de contenido

- [Stack](#stack)
- [Arquitectura general](#arquitectura-general)
- [Arquitectura de cada servicio](#arquitectura-de-cada-servicio)
- [Organizacion de carpetas](#organizacion-de-carpetas)
- [Levantar el proyecto](#levantar-el-proyecto)
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
| Base de datos | PostgreSQL 17 + Spring Data JPA | Una base por servicio. Las tablas se crean solas (`ddl-auto=update`); migraciones con Flyway *(planificado)* |
| Contenedores | Docker + Docker Compose | Entorno unico para el equipo; un `docker-compose.yml` por servicio y otro en la raiz |
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
| `servicio-tickets` | Nucleo: crear tickets, prioridad, asignacion, estados (falta: categoria e historial de cambios) | 8080 | Entidades + endpoints basicos + su PostgreSQL | por asignar |
| `servicio-estadisticas` | Estadisticas por estado (falta: categoria, prioridad, tiempo promedio, carga de tecnicos) | 8082 | Entidades + endpoints basicos + su PostgreSQL | por asignar |
| `servicio-identidad` | Usuarios y roles (falta: inicio de sesion y permisos) | 8083 | Entidades + endpoints basicos + su PostgreSQL | por asignar |
| `servicio-comunicacion` | Comentarios y notificaciones de cambio de estado, avisos pendientes o leidos | 8084 | Entidades + endpoints basicos + su PostgreSQL | por asignar |

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
├─ model/                            Entidades (tablas): Ticket, Prioridad, EstadoTicket
├─ repository/                       TicketRepository: guarda y consulta en PostgreSQL
├─ service/                          TicketService: la logica
└─ controller/                       TicketController: recibe la peticion y llama al servicio
```

- **`model/`:** las entidades. Son clases simples (campos, constructor vacio y getters/setters) que a la vez
  son la tabla en PostgreSQL y el JSON de Postman.
- **`repository/`:** una interfaz vacia que extiende `JpaRepository`; Spring genera el acceso a la base.
- **`service/`:** la logica. Valida lo basico y responde 400 / 404 / 409 con el motivo del error.
- **`controller/`:** los endpoints. No tiene logica, solo traduce HTTP al servicio.
- **Entre servicios:** referencias solo por id y ninguna llamada a otro servicio al crear algo, para que si
  uno cae los demas sigan trabajando.

**Por que tan simple:** el objetivo ahora es que todo el equipo pueda levantar el servicio y probarlo con
Postman. Cuando un servicio crezca (reglas de negocio, muchos endpoints) se separa en capas,
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
├─ docker-compose.yml             Levanta TODOS los servicios juntos con un solo PostgreSQL (4 bases)
├─ docker/postgres/               Script que crea las 4 bases y sus usuarios
├─ docs/
│  └─ adr/                        Decisiones de arquitectura (ADR)
└─ services/                      Un subdirectorio por microservicio (cada uno independiente)
   ├─ servicio-tickets/           App en el puerto 8080
   ├─ servicio-identidad/         App en el puerto 8083
   ├─ servicio-comunicacion/      App en el puerto 8084
   └─ servicio-estadisticas/      App en el puerto 8082
```

Dentro de cada servicio:

```
<servicio>/
├─ pom.xml                        Proyecto Maven propio
├─ Dockerfile                     Imagen del servicio (JRE ligero, memoria acotada)
├─ docker-compose.yml             Levanta SOLO este servicio con SU PostgreSQL
└─ src/
   ├─ main/java/com/helpdesk/<nombre>/
   │  ├─ Servicio<Nombre>Application.java
   │  ├─ model/                   Entidades (tablas)
   │  ├─ repository/              Acceso a la base de datos
   │  ├─ service/                 Logica
   │  └─ controller/              Endpoints
   ├─ main/resources/application.properties    Nombre, puerto y conexion a la base
   └─ test/java/                  Tests del servicio (sin base de datos)
```

Se agregara en las siguientes fases *(planificado)*: POM padre, `.env.example`, `scripts/` y los
workflows de CI (`ci.yml`, `docker.yml`).

Convenciones de nombres:

- Carpetas y archivos en **minusculas con guiones**, **sin tildes ni enie** (`estadisticas`, no `estadísticas`):
  las tildes dan problemas de codificacion entre Windows, macOS y Linux.
- Servicios en espanol, nombrados `servicio-<dominio>` (`servicio-tickets`, `servicio-identidad`...).
- Paquete de Java: `com.helpdesk.<dominio>` (`com.helpdesk.identidad`); el nombre de la carpeta debe coincidir
  con la linea `package` de cada clase.
- Clase de arranque: `Servicio<Dominio>Application` (el sufijo `Application` es la convencion de Spring Boot).

## Levantar el proyecto

**Requisitos:** solo Docker (Docker Desktop en Windows y macOS; Docker Engine con el plugin Compose en Linux).
No hace falta instalar Java, Maven ni PostgreSQL. Dale a Docker al menos 2 GB de memoria.

Cada servicio tiene **su propia base de datos PostgreSQL**. Hay tres formas de trabajar, de la mas liviana
a la mas completa:

### Opcion 1: solo tu servicio, con su base (la recomendada para trabajar)

Cada quien levanta unicamente el suyo. Desde la carpeta de tu servicio:

```bash
cd services/servicio-tickets        # tu carpeta
docker compose up -d --build        # levanta TU servicio y SU PostgreSQL
```

| Servicio | App | PostgreSQL (desde tu equipo) | Base / usuario / clave |
|---|---|---|---|
| `servicio-tickets` | `localhost:8080` | `127.0.0.1:5433` | `tickets_db` / `tickets` / `tickets` |
| `servicio-identidad` | `localhost:8083` | `127.0.0.1:5434` | `identidad_db` / `identidad` / `identidad` |
| `servicio-comunicacion` | `localhost:8084` | `127.0.0.1:5435` | `comunicacion_db` / `comunicacion` / `comunicacion` |
| `servicio-estadisticas` | `localhost:8082` | `127.0.0.1:5436` | `estadisticas_db` / `estadisticas` / `estadisticas` |

Comandos utiles (siempre desde la carpeta del servicio):

```bash
docker compose logs -f app          # ver los mensajes de la aplicacion
docker compose up -d --build app    # recompilar y reiniciar solo la app despues de cambiar codigo
docker compose down                 # apagar (los datos se conservan)
docker compose down -v              # apagar y BORRAR los datos
```

Los datos **se conservan** entre reinicios gracias al volumen de PostgreSQL. Las claves de arriba son solo
para desarrollo local; la base solo escucha en `127.0.0.1`, nadie mas de la red puede entrar.

### Opcion 2: solo la base en Docker y la app desde tu editor (la mas ligera)

```bash
cd services/servicio-tickets
docker compose up -d db             # solo PostgreSQL (unos 30 MB)
```

Luego ejecutas la aplicacion desde tu IDE (requiere JDK 21) con `ServicioTicketsApplication`. Ya apunta por
defecto a la base de la tabla de arriba, no hay que configurar nada.

### Opcion 3: todos los servicios juntos (para integrar)

Desde la raiz del repositorio. Usa **una sola instancia de PostgreSQL con 4 bases separadas** (cada servicio
con su usuario, y ninguno puede entrar a la base de otro), lo que gasta menos que 4 instancias:

```bash
docker compose up -d --build                                  # los 4 servicios + PostgreSQL
docker compose up -d servicio-tickets servicio-comunicacion   # solo algunos (y la base)
docker compose down                                           # apagar (los datos se conservan)
docker compose down -v                                        # apagar y BORRAR los datos
```

No uses la opcion 3 junto con la 1 al mismo tiempo: comparten los puertos de las aplicaciones. Si cambias
`docker/postgres/crear-bases.sql`, recrea los datos con `down -v`.

### Consumo de recursos

Medido con `docker stats` justo despues de arrancar (con uso intenso sube algo):

| Modo | Contenedores | Memoria aprox. |
|---|---|---|
| Opcion 2: solo la base | 1 | 30 MB (mas tu editor) |
| Opcion 1: un servicio y su base | 2 | 200 MB |
| Opcion 3: un solo servicio desde la raiz | 2 | 180 MB |
| Opcion 3: los 4 servicios juntos | 5 | 740 MB |

Lo que se hizo para que sea liviano:

- Imagen final con JRE minimo (no Maven ni JDK) y usuario sin privilegios.
- Cada app limitada: `-Xmx200m`, recolector `SerialGC` y tope de 320 MB por contenedor.
- Cada servicio abre solo 3 conexiones a la base (`DB_CONEXIONES`), y PostgreSQL esta recortado
  (`shared_buffers` bajo, pocas conexiones).
- En modo conjunto, un solo PostgreSQL en lugar de cuatro.

Consejos: trabaja con la opcion 1 o 2 y levanta la 3 solo para integrar; apaga con `docker compose down`
cuando termines; en Windows/macOS limita la memoria de Docker Desktop (Ajustes > Resources).

### Como se coordina el equipo

- Cada persona trabaja y prueba **su servicio solo** (opcion 1 o 2), sin esperar a nadie.
- Lo unico que comparten es el **contrato**: puertos, rutas y el aviso de cambio de estado, escritos en este
  README. Mientras cada uno lo respete, las piezas encajan.
- Para **integrar**: `git pull origin dev` y opcion 3 en una maquina, y se recorre el guion de la seccion
  siguiente. Conviene hacerlo al final de cada semana.
- **Estado actual:** los servicios todavia no se llaman entre si; el aviso de cambio de estado se envia a mano
  con Postman *(llamadas reales entre servicios y entre maquinas: planificado)*.

## Probar con Postman

Con el servicio levantado (cualquiera de las opciones anteriores), estos son los endpoints:

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

- **Base de datos:** PostgreSQL, **una base por servicio**. Las tablas se crean solas al arrancar
  (`ddl-auto=update`), suficiente para el prototipo; el siguiente paso son migraciones versionadas con Flyway
  *(planificado)*. Las claves de las bases son de desarrollo; la conexion se puede cambiar con las variables
  `DB_URL`, `DB_USUARIO`, `DB_CLAVE` y `DB_CONEXIONES`. El servicio de tickets usa H2 en memoria solo en su
  test de arranque.
- **Referencias entre servicios:** solo por id (`solicitanteId`, `ticketId`...), sin llave foranea, porque la
  tabla referenciada esta en la base de otro servicio.
- **Comunicacion:** REST sincrono al principio. Cuando haga falta desacoplar (por ejemplo,
  notificar al crear un ticket), se agregara mensajeria por eventos.
- **Salud:** exponer `/actuator/health` en cada servicio, para que Docker Compose sepa cuando esta listo
  *(planificado)*.

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
- [x] **Fase 1, servicios simples con su base:** los cuatro servicios con entidades, endpoints basicos y su propio PostgreSQL, levantables de a uno o todos juntos.
- [ ] **Fase 1b, infraestructura comun:** POM padre, Flyway y comprobaciones de salud (`/actuator/health`).
- [ ] **Fase 2, CI:** workflow de build y tests por ruta; ejecutarlo en local con [`act`](https://github.com/nektos/act).
- [ ] **Fase 3, desarrollar los servicios:** cada responsable completa el suyo (campos que faltan, base de datos, y capas si el servicio crece).
- [ ] **Fase 4, gateway y seguridad:** API Gateway, autenticacion JWT.
- [ ] **Fase 5, frontend.**
