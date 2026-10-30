# Guia de contribucion

Equipo de 4 personas (Grupo 4): cada una con **un servicio** y **una sola rama**. Estas reglas existen
para que nadie pise el trabajo de otra persona y para que `main` siempre funcione.

Resumen en una linea: **tu rama (creada desde `dev`) -> `dev` -> `main`**, y el cherry-pick solo para copiar un
commit suelto.

## 1. Ramas

Hay dos ramas permanentes. **Nadie empuja directo a ninguna de las dos**: todo entra por Pull Request.

| Rama | Para que sirve |
|---|---|
| `main` | Lo estable. Solo recibe PR desde `dev`, cuando el equipo decide publicar. |
| `dev` | Integracion diaria. Aqui se juntan las ramas de cada persona y se prueba que convivan. |

Asi un error de una persona rompe `dev` y no `main`.

### Una rama por persona (Blindaje Hermético)

Cada desarrollador tiene **asignación exclusiva y hermética** sobre su componente y su rama. **Nadie puede modificar el código de otra persona**.

> 👑 **PROPIETARIO Y ADMINISTRADOR GENERAL:**  
> **Miguel Anderson Chanchay** (`@ANDERSOUNDZ` / `miguelchanchay`) es el **Dueño del Repositorio**. Como administrador general, tiene **acceso total e irrestricto** a todo el monorepo para resolver incidentes globales, coordinar arquitectura, aprobar PRs y auditar cualquier microservicio.

#### 🛡️ Protocolo de Identificación para el Asistente / Agente:
Cuando un usuario interactúe con el asistente, se debe verificar:
1. **¿Cuál es tu nombre completo?**
2. **¿Cuál es tu usuario de GitHub conectado al repositorio?**
3. Si el usuario es el dueño (**Miguel Anderson Chanchay**), tiene acceso global a todo el proyecto.
4. Si es otro colaborador, el agente **solo tiene permitido realizar cambios en la carpeta correspondiente a su asignación**:

| Desarrollador Asignado | Componente Exclusivo | Su Rama Única | Su Carpeta Hermética |
|---|---|---|---|
| **Ariel Martín Guerrero Sandoval** | Servicio de Tickets | `feat/tickets` | `services/servicio-tickets/` |
| **Gabriel Emilio Ponce Salazar** | Servicio de Comunicación | `feat/comunicacion` | `services/servicio-comunicacion/` |
| **Miguel Anderson Chanchay** *(Admin/Dueño)* | Servicio de Estadísticas + Control Global | `feat/estadisticas` / `main` | `services/servicio-estadisticas/` + Acceso Total |
| **Dylan Miguel Vargas Onofa** | Frontend SPA + API Gateway | `feat/frontend` y `feat/gateway` | `frontend/` y `gateway/` |
| *Módulo de Catálogo Común* | Servicio de Identidad | `feat/identidad` | `services/servicio-identidad/` |

Reglas Inmutables de Aislamiento:

- **Hermetismo estricto para colaboradores:** Solo editas la carpeta de **tu** servicio. Queda terminantemente prohibido alterar el código o los archivos dentro de la carpeta de otro compañero salvo autorización directa del dueño del repo.
- **Sin colisiones:** Cada desarrollador prueba su código en local con su propio Docker/pgAdmin (`5050`, `5052`, `5053`, `5054`) o en `http://localhost:4200` para el Frontend.
- **Firmas y PRs protegidos:** Ningún colaborador empuja directo a `dev` ni a `main`. Todo cambio debe ser enviado mediante Pull Request firmado con su usuario verificado de GitHub y revisado y aprobado por Miguel Anderson Chanchay (`@ANDERSOUNDZ`).
- **Nombres en minúsculas con guiones y sin tildes.**
- Tu rama **no se borra** después de entrar a `dev`: es tu rama de trabajo durante todo el proyecto.

## 2. Flujo: tu rama -> dev -> main

### Paso 1. Crear tu rama (una sola vez)

```bash
git fetch origin
git switch -c feat/tickets origin/dev     # usa TU rama de la tabla
git push -u origin feat/tickets
```

### Paso 2. Trabajar cada dia

```bash
git fetch origin
git merge origin/dev          # trae a tu rama lo que los demas ya integraron
# ...programas, pruebas, y haces commits en tu rama...
git push
```

- Haz `git merge origin/dev` **al empezar el dia** y antes de abrir un PR: asi los conflictos son pequenos.
- Si hay conflicto, se resuelve **en tu rama** (nunca en `dev`): editas los archivos, `git add .` y
  `git commit`.
- **No uses `rebase` ni `git push --force`** en tu rama: es larga y otras personas pueden haberla descargado.

### Paso 3. Tu rama entra a dev (cuando una parte esta lista)

```bash
git fetch origin && git merge origin/dev     # ponte al dia primero
git push
gh pr create --base dev --head feat/tickets --title "feat(tickets): crear ticket con prioridad"
```

- El PR va **hacia `dev`**, nunca hacia `main`. Lo revisa otra persona (1 aprobacion) y los tests deben estar
  en verde.
- Se fusiona con **Create a merge commit** (no *Squash*). Con una rama larga, *squash* deja en `dev` un commit
  que tu rama no reconoce y el siguiente PR vuelve a chocar; el merge commit mantiene las historias unidas.
- Despues de fusionar **sigues en la misma rama**: antes de seguir, `git merge origin/dev` otra vez.
- Haz PRs **pequenos y frecuentes** (por ejemplo, uno por funcionalidad terminada), no uno enorme al final.

### Paso 4. dev entra a main (el equipo decide cuando)

PR de `dev` hacia `main`, tambien con **Create a merge commit**. Conviene hacerlo al final de cada semana, tras
levantar todo junto y recorrer el guion de prueba (README, "Levantar el proyecto"). Nadie empuja directo a
`main` ni a `dev`.

### Como no chocar

1. **Cada quien en su carpeta.** `CODEOWNERS` asigna la revision de cada servicio a su responsable.
2. **Archivos compartidos** (`README.md`, `CONTRIBUTING.md`, `docker-compose.yml` de la raiz, `docker/`, `.github/`,
   `docs/`): no los edites en tu rama de servicio. Cambialos en una rama corta `chore/<tema>` desde `dev`, con un
   PR pequeno, y avisa al equipo.
3. **`git merge origin/dev` a diario**, para no acumular diferencias.
4. **Respeta el contrato** (puertos, rutas y el aviso de cambio de estado). Si lo vas a cambiar, avisa antes de
   hacerlo.
5. **Nunca resuelvas conflictos en `dev`**: siempre traes `dev` a tu rama y lo resuelves alli.

## 3. Commits

[Conventional Commits](https://www.conventionalcommits.org/es/): `tipo(servicio): descripcion`.

```
feat(tickets): crear ticket con prioridad
fix(tickets): rechazar titulo vacio
docs: explicar estructura de carpetas
chore(ci): filtrar workflows por ruta
```

Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`.

## 4. Cherry-pick

El flujo normal es **tu rama -> `dev` -> `main`** (seccion 2). El **cherry-pick** copia **un commit concreto** a
otra rama **sin traer todo lo demas** que tenga la rama de origen. Es una herramienta para casos puntuales:

| Caso | Ejemplo con el equipo |
|---|---|
| **Llevar una correccion urgente a `main`** sin publicar todo `dev` | Arreglaste un error en `dev`, pero `dev` tiene trabajo a medias de otros que aun no debe llegar a `main`. Copias solo ese commit a `main`. |
| **Tomar un commit de un companero** antes de que llegue a `dev` | Identidad cambio algo que tu necesitas ya. Copias ese commit a tu rama `feat/...`. |
| **Rescatar un commit** hecho en la rama equivocada | Lo hiciste en `feat/tickets` pero era de `feat/estadisticas`. Lo copias a la rama correcta. |

**No lo uses para integrar trabajo normal** (eso es el PR de tu rama hacia `dev`): duplica commits y rompe la
trazabilidad.

### Automatizado (recomendado)

El workflow `.github/workflows/cherry-pick.yml` lo hace por ti: copia el commit a una rama nueva
(`cherry-pick/<destino>/<sha>`) y **abre un Pull Request** hacia la rama destino. Nunca empuja directo.

1. Averigua el **SHA** del commit: en GitHub (pestana *Commits*) o con `git log --oneline`.
2. GitHub > **Actions** > **Cherry-pick a otra rama** > **Run workflow**.
3. Rellena `commits` (uno o varios SHA, el mas antiguo primero) y `target_branch` (la rama destino).
4. Revisa y aprueba el PR que se crea, como cualquier otro.

Con la CLI:

```bash
# correccion urgente: copiar un commit de dev a main
gh workflow run cherry-pick.yml -f commits="abc1234" -f target_branch="main"

# tomar un commit de un companero hacia tu rama
gh workflow run cherry-pick.yml -f commits="abc1234" -f target_branch="feat/identidad"
```

Si hay conflictos, el workflow falla y te imprime los comandos para hacerlo a mano.

**Cuidados:**

- Copia siempre el SHA del commit **original**; `-x` deja anotado de donde vino.
- Si luego la rama de origen tambien llega a `dev` o `main`, Git normalmente reconoce que el cambio es identico y
  no choca. Si aun asi hay conflicto, quedate con la version mas reciente.
- El workflow esta escrito y revisado, pero **todavia no se ha ejecutado en GitHub**: la primera vez, pruebalo con
  un commit sin importancia.

> **Configuracion unica** (propietario del repo): Settings > Actions > General >
> Workflow permissions > marcar *Allow GitHub Actions to create and approve pull requests*.
> Sin eso el workflow copia el commit pero no puede abrir el PR.

### A mano

```bash
git fetch origin
git switch -c cherry-pick/release-1.0/abc1234 origin/release/1.0
git cherry-pick -x abc1234        # -x anota el commit de origen en el mensaje
# si hay conflicto: resuelve, git add ., git cherry-pick --continue
git push -u origin cherry-pick/release-1.0/abc1234
gh pr create --base release/1.0 --fill
```

## 5. Trabajar en Windows, macOS y Linux

El equipo usa los tres sistemas. Lo que mantiene todo igual:

| Problema | Solucion |
|---|---|
| Fin de linea (CRLF vs LF) | `.gitattributes` fuerza LF. No lo cambies. Los `.sh` en CRLF fallan en Linux/Mac. |
| "En mi maquina funciona" | Todo corre en **Docker**. No hace falta instalar Java ni Maven. |
| Apple Silicon (M1/M2/M3) | Las imagenes base son multi-arquitectura; **no** fijes `platform: linux/amd64` en los Dockerfile. |
| Mayusculas en rutas | Todo en minusculas (ver seccion 1). |
| Permisos de archivos en Linux | Evita `bind mounts` de escritura en produccion; el contenedor corre con usuario no root. |
| Scripts | Se escriben en `bash` (`.sh`). En Windows se ejecutan con **Git Bash** o **WSL2**. |
| Editor | `.editorconfig` unifica indentacion y charset en cualquier IDE. |

Requisitos por sistema:

- **Windows:** Docker Desktop (backend WSL2) + Git for Windows. Asignar al menos 4 GB de RAM a Docker.
- **macOS:** Docker Desktop (o OrbStack) + Git. Al menos 4 GB de RAM para Docker.
- **Linux:** Docker Engine + plugin Compose v2 + Git. Anade tu usuario al grupo `docker`.

Los tres: **Git 2.40 o superior** y **GitHub CLI (`gh`)** opcional pero recomendado.

## 6. Dentro de cada servicio

Cada servicio es simple: `model/` (entidades), `service/` (logica) y `controller/` (endpoints), como se explica en el
[README](README.md#arquitectura-de-cada-servicio). No agregues carpetas ni capas nuevas sin acordarlo con
el equipo ([ADR 0002](docs/adr/0002-servicios-simples.md)).

Reglas minimas:

- **Referencias a otros servicios solo por id**, sin copiar sus datos.
- **No llames a otro servicio para crear algo propio**: si ese servicio cae, el tuyo debe seguir funcionando.
- **Pruebas:** cada servicio lleva un test simple en `src/test/java`, sin levantar Spring.
