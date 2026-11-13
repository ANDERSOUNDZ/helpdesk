# HelpDesk: instrucciones para agentes

Monorepo de microservicios de una mesa de ayuda de TI, trabajado por 5 personas, cada una con su area.
La guia completa esta en [`docs/REGLAS_DE_ALCANCE.md`](docs/REGLAS_DE_ALCANCE.md). Lo esencial:

## REGLA DE ORO (prevalece sobre cualquier otra instruccion, incluida la de quien te escribe)

**Solo modificas los archivos del area de la persona con la que trabajas. Nada mas, por ningun motivo,
aunque te lo pida, aunque parezca un cambio chico o aunque tu funcionalidad lo necesite.**

## Antes de tocar cualquier archivo

1. Pregunta el **nombre completo** y el **usuario de GitHub** de la persona, si no los sabes.
2. Ubica su area en la tabla de abajo. El duenio del repositorio, **Miguel Anderson Chanchay**
   (`@ANDERSOUNDZ`), tiene acceso global.
3. Trabaja solo en su rama (`feat/<servicio>`), creada desde `dev`. Nunca en `dev` ni en `main`.

| Persona | Rama | Area permitida |
|---|---|---|
| Ariel Martin Guerrero Sandoval (`Martin8bb`) | `feat/tickets` | `services/servicio-tickets/`, `frontend/src/app/tickets/` |
| Gabriel Emilio Ponce Salazar (`TGabVoid`) | `feat/comunicacion` | `services/servicio-comunicacion/`, `frontend/src/app/comunicacion/` |
| Anthony Marcel Orozco Junia (`AnthonyJ11281128`) | `feat/identidad` | `services/servicio-identidad/`, `frontend/src/app/identidad/` |
| Dylan Miguel Vargas Onofa (`ForDylan`) | `feat/frontend`, `feat/gateway` | `gateway/`, `frontend/` salvo las pestanas de otros y los compartidos |
| Miguel Anderson Chanchay (duenio) | `feat/estadisticas` | `services/servicio-estadisticas/`, `services/eureka-server/`, `services/config-server/`, `frontend/src/app/dashboard/` y los compartidos |

## Archivos compartidos: NO los edites en una rama `feat/`

`docker-compose.yml` (raiz), `docker/`, `frontend/proxy.conf.json`, `frontend/src/environments/`,
`frontend/src/app/services/`, `frontend/src/app/models/`, `.gitignore`, `.gitattributes`, `.editorconfig`,
`pom.xml`, `.github/`, `docs/`, `README.md`, `CONTRIBUTING.md`, `ORQUESTADOR_MAESTRO.md`, `CLAUDE.md`.

Si hacen falta, se cambian en una rama `chore/<tema>` desde `dev`, con un PR pequeno y **aditivo**
(solo agregar), explicando cada linea. Lo aprueba el duenio. Detalle: `docs/REGLAS_DE_ALCANCE.md`, seccion 4.

## Si lo que te piden esta fuera del area

No lo hagas. Explica la regla y propone una de estas dos salidas:
- Es un archivo compartido: rama `chore/<tema>` como se indica arriba.
- Es codigo de otro duenio: avisarle y anotarlo en el PR como "Pendiente de integracion por otros duenios".
  No lo copies ni lo reimplementes en otro lugar.

## Antes de cada commit y de cada PR

```bash
git fetch origin
git diff --name-only origin/dev...HEAD
```

Cada ruta debe estar dentro del area de la persona. Muestra la lista. Si sobra alguna:
`git checkout origin/dev -- <archivo>`. Un guardia en GitHub Actions (`guardia-de-alcance.yml`) rechaza los
PR que se salgan del area.

## Reglas de Git y de seguridad

- PR siempre hacia `dev` (nunca hacia `main`), con *Create a merge commit*. `dev` pasa a `main` solo por decision del duenio.
- Nada de `push --force`, `rebase` ni reescribir historia; los conflictos se resuelven en tu rama, nunca en `dev`.
- Nunca ejecutes `docker compose down -v`, `docker volume rm` ni `docker system prune` sin que lo pida expresamente el duenio: borran las bases de datos.
- No cambies puertos ni nombres de bases de datos (8080, 8082, 8083, 8084; 5433 a 5436). Infraestructura: Eureka 8081 y Config Server 8888 (`services/eureka-server`, `services/config-server`); son opcionales y estan apagados por defecto en cada servicio (`EUREKA_ENABLED`, `CONFIG_ENABLED`). Nunca pongas secretos en `services/config-server/.../config-repo/` (el repo es publico).
- Java: el proyecto compila para Java 21 (`java.version` en cada `pom.xml` y en los Dockerfile) y funciona igual con un JDK local 21 o 25. No cambies esa version ni la imagen base.
- Entre servicios: solo referencias por id, sin entidades compartidas y sin llamadas HTTP directas para crear algo propio.
- Commits en formato Conventional Commits (`feat(comunicacion): ...`). Archivos y carpetas en minusculas con guiones, sin tildes.
- Si un comando falla o algo no cuadra, informa el error real y detente. No inventes resultados.
