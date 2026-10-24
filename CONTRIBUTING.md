# Guia de contribucion

Equipo de 5 personas, cada una con sus servicios y sus ramas. Estas reglas existen
para que nadie pise el trabajo de otra persona y para que `main` siempre funcione.

## 1. Ramas

Hay dos ramas permanentes. **Nadie empuja directo a ninguna de las dos**: todo entra por Pull Request.

| Rama | Para que sirve |
|---|---|
| `main` | Lo estable. Solo recibe PR desde `dev`, cuando el equipo decide publicar. |
| `dev` | Integracion diaria. Aqui se juntan las ramas de cada persona y se prueba que convivan. |

Asi un error de una persona rompe `dev` y no `main`.

| Tipo | Formato | Ejemplo |
|---|---|---|
| Funcionalidad | `feat/<servicio>/<descripcion>` | `feat/tickets/crear-ticket` |
| Correccion | `fix/<servicio>/<descripcion>` | `fix/tickets/validar-prioridad` |
| Infra / docs | `chore/<tema>` o `docs/<tema>` | `chore/ci-por-rutas` |
| Release (si hace falta) | `release/<version>` | `release/1.0` |

Reglas:

- Nombres en **minusculas con guiones** (`crear-ticket`, no `CrearTicket`). Linux es
  sensible a mayusculas y macOS/Windows no; mezclarlas rompe el repo en una de las dos.
- Ramas **cortas**: idealmente 1 a 3 dias. Una rama que vive semanas termina en conflictos.
- Cada persona trabaja en **su** servicio. Si necesitas tocar el de otra persona, avisale
  y explicalo en el PR.

## 2. Flujo diario

```bash
git fetch origin
git switch -c feat/tickets/crear-ticket origin/dev   # tu rama sale de dev

# ...trabajas y haces commits...

git fetch origin
git rebase origin/dev                        # ponte al dia ANTES de abrir el PR
git push -u origin feat/tickets/crear-ticket
gh pr create --base dev --fill               # el PR va hacia dev, no hacia main
```

- El PR requiere **1 aprobacion** (la del dueno del servicio via `CODEOWNERS`) y el CI en verde.
- Se integra con **Squash and merge**: queda un commit limpio por funcionalidad en `dev`.
- Tras el merge, borra tu rama.
- Para publicar: PR de `dev` hacia `main`, con **Create a merge commit** (no squash), para que
  `dev` y `main` no se desalineen.

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

El flujo normal es **PR hacia `dev`**. El cherry-pick es para casos puntuales:

- Llevar un **hotfix** de `main` a una rama `release/*`.
- Rescatar **un commit suelto** de otra rama.

No lo uses para integrar trabajo normal: duplica commits y rompe la trazabilidad.

### Automatizado (recomendado)

El workflow `.github/workflows/cherry-pick.yml` lo hace por ti y abre un PR:

1. Copia el SHA del commit (en `main`, tras un squash merge es un solo commit).
2. GitHub > **Actions** > **Cherry-pick a otra rama** > **Run workflow**.
3. Rellena `commits` (uno o varios SHA, el mas antiguo primero) y `target_branch`.
4. Revisa y aprueba el PR que se crea, como cualquier otro.

Con la CLI:

```bash
gh workflow run cherry-pick.yml -f commits="abc1234" -f target_branch="release/1.0"
```

Si hay conflictos, el workflow falla y te imprime los comandos para hacerlo a mano.

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
