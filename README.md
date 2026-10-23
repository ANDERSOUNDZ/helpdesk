# HelpDesk

Plataforma de tickets de soporte construida como **microservicios** con **arquitectura Clean**.
Primero el backend (Spring Boot); el frontend vendra despues.

> **Estado: fase 0, fundacion.** Hoy el repositorio define la estructura, las reglas de equipo
> y el flujo de trabajo. Los servicios aun no existen; lo que esta marcado como *(planificado)*
> mas abajo se ira construyendo por fases (ver [Hoja de ruta](#hoja-de-ruta)).

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
          +-------------------------------------------+
          |  API Gateway   (fuera de alcance por ahora)|
          +-------------------------------------------+
               |                |                |
               v                v                v
        +-------------+  +-------------+  +-------------+
        |   tickets   |  |    users    |  |  comments   |   ... un servicio
        |   service   |  |   service   |  |   service   |       por dominio
        +------+------+  +------+------+  +------+------+
               |                |                |
               v                v                v
        +-------------+  +-------------+  +-------------+
        |  tickets_db |  |  users_db   |  | comments_db |   una BD por servicio
        +-------------+  +-------------+  +-------------+
```

Principios:

1. **Un servicio por contexto de negocio** (tickets, usuarios, comentarios...).
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
   └─ .gitkeep                    (vacia por ahora)
```

Se agregara en las siguientes fases *(planificado)*:

```
helpdesk/
├─ pom.xml                        POM padre: versiones y dependencias comunes
├─ Dockerfile                     Unico y parametrizado: --build-arg SERVICE=tickets-service
├─ docker-compose.yml             Servicios + PostgreSQL
├─ .env.example                   Variables de entorno de ejemplo (sin secretos)
├─ scripts/                       Atajos en bash: build, test, up, ci-local
├─ .github/workflows/
│  ├─ ci.yml                      Build y tests por servicio, solo si cambio su carpeta
│  └─ docker.yml                  Construccion de imagenes
└─ services/
   └─ tickets-service/            Primer servicio: plantilla para los demas
      ├─ pom.xml
      └─ src/
         ├─ main/java/com/helpdesk/tickets/    (ver estructura de paquetes arriba)
         ├─ main/resources/
         │  ├─ application.yml
         │  └─ db/migration/      Scripts de Flyway: V1__crear_tickets.sql
         └─ test/java/            Tests de dominio, casos de uso y ArchUnit
```

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

- `main` protegida; todo entra por **Pull Request** con 1 aprobacion y CI en verde.
- Una rama por tarea: `feat/<servicio>/<descripcion>`. Cada persona es responsable de sus servicios (`CODEOWNERS`).
- Integracion con **squash merge** y commits [Conventional Commits](https://www.conventionalcommits.org/es/).
- **Cherry-pick** automatizado y manual solo para hotfixes y rescates puntuales:
  GitHub > Actions > *Cherry-pick a otra rama*.

Todo el detalle esta en [CONTRIBUTING.md](CONTRIBUTING.md).

## Hoja de ruta

- [x] **Fase 0, fundacion:** repositorio, estructura, reglas de equipo, cherry-pick automatizado.
- [ ] **Fase 1, plantilla de servicio:** POM padre, `tickets-service` con Clean, Dockerfile, Compose con PostgreSQL.
- [ ] **Fase 2, CI:** workflow de build y tests por ruta; ejecutarlo en local con [`act`](https://github.com/nektos/act).
- [ ] **Fase 3, mas servicios:** usuarios, comentarios, notificaciones.
- [ ] **Fase 4, gateway y seguridad:** API Gateway, autenticacion JWT.
- [ ] **Fase 5, frontend.**
