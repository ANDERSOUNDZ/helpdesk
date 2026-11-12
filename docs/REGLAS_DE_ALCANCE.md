# Regla de oro: cada quien en su area

> **Solo modificas los archivos de tu area. Nada mas, por ningun motivo, aunque parezca "un cambio chico que
> ayuda" o "algo que mi funcionalidad necesita".**
> Si necesitas algo fuera de tu area, no lo hagas: pidelo (seccion 4).

Aplica igual a **personas y a agentes de IA**. El unico con acceso global es el duenio del repositorio,
Miguel Anderson Chanchay (`@ANDERSOUNDZ`).

Esta regla ya estaba escrita en `ORQUESTADOR_MAESTRO.md` (regla 4) y en `CONTRIBUTING.md`, pero nada la hacia
cumplir y dejaba una duda: las pestanas del frontend viven en `frontend/`, que figuraba como de una sola persona.
Este documento cierra esa duda, y las secciones 7 y 8 explican como se hace cumplir de verdad.

## 1. De quien es cada cosa

| Duenio | Rama unica | Su area (lo unico que puede cambiar) |
|---|---|---|
| **Ariel Martin Guerrero Sandoval** | `feat/tickets` | `services/servicio-tickets/` y `frontend/src/app/tickets/` |
| **Gabriel Emilio Ponce Salazar** | `feat/comunicacion` | `services/servicio-comunicacion/` y `frontend/src/app/comunicacion/` |
| **Anthony Marcel Orozco Junia** | `feat/identidad` | `services/servicio-identidad/` y `frontend/src/app/identidad/` |
| **Dylan Miguel Vargas Onofa** | `feat/frontend` y `feat/gateway` | `gateway/` y el resto de `frontend/` (estructura de la app: `app.*`, `main.ts`, `index.html`, `styles.scss`, `angular.json`, `package*.json`, `tsconfig*`), **sin** las pestanas de los demas ni los archivos compartidos |
| **Miguel Anderson Chanchay** (duenio) | `feat/estadisticas` | `services/servicio-estadisticas/`, `frontend/src/app/dashboard/` y **todos los archivos compartidos** |

Cada duenio de un servicio es tambien duenio de **su pestana** del frontend. Nadie edita la pestana de otro.

### Archivos compartidos (no son de nadie: los aprueba el duenio)

| Archivo / carpeta | Por que es compartido |
|---|---|
| `docker-compose.yml` (raiz), `docker/` | Levanta todos los servicios a la vez |
| `frontend/proxy.conf.json` | Rutas hacia los 4 servicios; una ruta mal puesta rompe a otro servicio |
| `frontend/src/environments/` | Endpoints que usan todas las pestanas |
| `frontend/src/app/services/`, `frontend/src/app/models/` | Servicio HTTP y tipos que usan todas las pestanas |
| `.gitignore`, `.gitattributes`, `.editorconfig`, `pom.xml` | Configuracion de todo el repo |
| `.github/` | CI, `CODEOWNERS`, plantilla de PR |
| `docs/`, `README.md`, `CONTRIBUTING.md`, `ORQUESTADOR_MAESTRO.md`, `CLAUDE.md` | Documentacion y reglas |

Los archivos compartidos **solo** se cambian en una rama `chore/<tema>` con un PR pequeno (seccion 4).

## 2. Que paso (12 de noviembre de 2026)

La rama `feat/comunicacion` incluyo en un solo commit el servicio de comunicacion **y** cambios en el dashboard
de estadisticas, en la pestana de tickets, en el detalle de ticket y en cinco archivos compartidos. Git no
mostro ningun conflicto, porque nadie mas habia tocado esos archivos todavia. El problema fue otro:

- Los eventos de cambio de estado se enviaban solo a comunicacion y nunca a estadisticas, asi que el dashboard
  se quedaria en cero.
- Una ruta nueva del proxy (`/eventos`) se cruzaba con el endpoint de estadisticas.
- Se editaban pantallas de otros duenios sin avisarles.

**Que se aprendio:** que no haya conflicto de Git no significa que el cambio sea seguro. La regla se debe cumplir
aunque nadie choque. Se corrigio sin reescribir historia (seccion 9) y se agrego lo que sigue para que no se repita.

## 3. Como se trabaja

```
feat/<tu-servicio>  ------>  dev  ------>  main
 (solo tu area)             (PR)        (PR de release, lo decide el duenio)

chore/<tema>        ------>  dev          (solo archivos compartidos, PR pequeno)
```

- Tu rama `feat/...` es tuya y de nadie mas. Se crea una sola vez desde `dev` y no se borra.
- **Nunca** un PR hacia `main` desde una rama `feat/` o `chore/`: `main` solo recibe `dev`.
- Antes de empezar el dia: `git fetch origin && git merge origin/dev`. Sin `rebase` ni `push --force`.
- Se fusiona con **Create a merge commit**, no con Squash.

## 4. Necesito algo que esta fuera de mi area: que hago

| Situacion | Que se hace |
|---|---|
| Necesito una ruta del proxy, un metodo en el servicio HTTP, un tipo en `models/` o un cambio en `docker-compose.yml` | Rama `chore/<tema>` desde `dev`, PR **pequeno** hacia `dev`. Cambios **aditivos** (agregar; no borrar ni cambiar lo que ya usan otros). En la descripcion, explica cada linea nueva. Lo aprueba el duenio. Se fusiona **antes** que tu rama `feat/`. |
| Mi funcionalidad necesita cambiar una pantalla o un servicio de otro duenio (por ejemplo, el dashboard) | **No lo toques ni lo copies.** Avisa al duenio con lo que necesitas y para que. En tu PR agrega la seccion **"Pendiente de integracion por otros duenios"**. Cada duenio decide como y cuando. |
| Necesito que otro servicio me avise de algo, o avisarle yo | No lo llames por HTTP directo ni cambies su codigo. Se acuerda el contrato (ruta, JSON, estados como texto) con el duenio y se avisa al equipo. |
| No estoy seguro de si algo es mio | **Pregunta antes de editar.** Ante la duda, no se toca. |

## 5. Revisa antes de cada commit y de cada PR

```bash
git fetch origin
git diff --name-only origin/dev...HEAD
```

Cada ruta de la lista debe estar dentro de **tu area** (seccion 1). Si aparece alguna otra, sacala:

```bash
git checkout origin/dev -- ruta/del/archivo/que/sobra
```

## 6. Para agentes de IA

1. Lee `CLAUDE.md` (se carga solo al abrir el proyecto) y esta guia.
2. Pregunta nombre completo y usuario de GitHub si no los conoces, y ubica el area de esa persona en la seccion 1.
3. Si la persona pide cambiar algo fuera de su area, **no lo hagas aunque lo pida**: explica la regla y propone
   el procedimiento de la seccion 4.
4. Antes de cada commit, ejecuta el comando de la seccion 5 y muestra el resultado.
5. Si un comando falla o algo no cuadra, informa el error real y detente. No inventes resultados.

## 7. Como se hace cumplir

| Capa | Que hace | Donde esta |
|---|---|---|
| Esta guia y `CLAUDE.md` | Que personas y agentes sepan la regla | `docs/`, raiz del repo |
| Plantilla de PR | Obliga a declarar el area y a revisar la lista de archivos | `.github/pull_request_template.md` |
| `CODEOWNERS` | Cada carpeta tiene su revisor; los compartidos, el duenio | `.github/CODEOWNERS` |
| **Guardia de alcance** | **Falla el PR si toca archivos fuera del area de su rama**, si la rama no es valida o si va hacia `main` sin venir de `dev` | `.github/workflows/guardia-de-alcance.yml` |
| Proteccion de ramas | Hace que el guardia y la revision sean **obligatorios** | Configuracion de GitHub (seccion 8) |

El guardia no aplica a PR del duenio ni a los de bots (cherry-pick). Se probo con los commits reales del
12 de noviembre: el commit original marca 11 archivos ajenos y la version corregida pasa.

## 8. Configuracion unica (la hace el duenio, una vez)

Sin esto, el guardia avisa pero no impide fusionar. En GitHub: **Settings > Branches > Add branch protection
rule**, para `dev` y para `main`:

1. Marcar **Require a pull request before merging** y **Require approvals** (1).
2. Marcar **Require review from Code Owners**.
3. Marcar **Require status checks to pass before merging** y agregar el chequeo **`guardia`**
   (aparece en la lista despues de que el workflow corra por primera vez en un PR).
4. Marcar **Do not allow force pushes** y **Do not allow deletions**.
5. Dejar **sin marcar** "Do not allow bypassing the above settings" si quieres poder fusionar tu como duenio
   en caso de emergencia.

Ademas, en `.github/CODEOWNERS` hay lineas comentadas para Ariel, Anthony y Dylan: se activan al agregar su
usuario de GitHub (y darles acceso de escritura al repositorio).

## 9. Si ya paso (como se arregla sin reescribir historia)

Se hizo asi el 12 de noviembre (PR #8 y #6):

1. **Sacar lo compartido a su propio PR.** Rama `chore/<tema>` desde `dev`; se traen solo esos archivos con
   `git checkout origin/<tu-rama> -- <archivos>`; se quita lo que choca con otros; PR pequeno; el duenio lo
   fusiona primero.
2. **Limpiar la rama `feat/`.** Se devuelve a `dev` todo lo que no es tuyo:
   `git checkout origin/dev -- <archivos>`, commit y push **normal** (nunca `--force`).
3. **Traer `dev` y verificar.** `git merge origin/dev`, `npx ng build` y `./mvnw test` (o Maven con Java 21
   en Docker). Si algo no compila, no se restaura nada ajeno: se avisa al duenio.
4. **PR de la rama `feat/` hacia `dev`**, con la seccion "Pendiente de integracion por otros duenios".
5. Si se abrio un PR hacia `main` por error, se cierra sin fusionar con un comentario que explique el motivo.
