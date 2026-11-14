# Flujo de trabajo: el orden de siempre

> **Regla inmutable del proyecto** (la "Regla uno" del `ORQUESTADOR_MAESTRO.md`, junto a la regla de alcance).

Este es el orden que sigue **cada persona y cada agente**, siempre, para no chocar con el trabajo de los demas.
Se complementa con [REGLAS_DE_ALCANCE.md](REGLAS_DE_ALCANCE.md) (que archivos puedes tocar) y con
[CONTRIBUTING.md](../CONTRIBUTING.md) (ramas, commits y cherry-pick).

```
1. Actualizar -> 2. Cambios y commits -> 3. Push -> 4. Pull Request -> 5. Revision -> 6. Merge -> 7. Actualizar
```

Cada persona tiene **una sola rama de trabajo**, creada una vez desde `dev` (`feat/tickets`, `feat/comunicacion`,
`feat/identidad`, `feat/frontend`, `feat/gateway`, `feat/estadisticas`). Los archivos compartidos van en una rama
corta `chore/<tema>` (se crea desde `dev` y se puede borrar al fusionar).

## 1. Actualizar tu rama (siempre primero)

```bash
git fetch origin
git switch feat/<tu-servicio>
git merge origin/dev            # trae lo que los demas ya integraron; los conflictos se resuelven aqui, nunca en dev
```

Sin `rebase` ni `push --force`. Para una rama `chore/` nueva: `git switch -c chore/<tema> origin/dev`.

## 2. Hacer los cambios y commitear

Commits **pequenos**, uno por paso logico, en formato `tipo(alcance): descripcion`
(`feat(tickets): validar tecnico al asignar`). Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`.

Antes del commit, comprueba que solo tocaste **tu area**:

```bash
git diff --name-only origin/dev...HEAD
```

## 3. Push

```bash
git push -u origin feat/<tu-servicio>      # normal, nunca --force
```

## 4. Abrir el Pull Request hacia `dev`

```bash
gh pr create --base dev --head feat/<tu-servicio> --title "feat(tickets): validar tecnico al asignar"
```

- El **titulo** sigue el formato de los commits (el guardia lo comprueba).
- Llena la plantilla: **que cambia, por que, como se probo**, lista de archivos y "pendiente de integracion".
- **Un PR, un proposito.** Si necesitas "y" para describirlo, son dos PR. Pequenos y frecuentes.
- **Nunca hacia `main`.** `main` solo recibe `dev`, por decision del duenio.
- Si tu PR **depende de otro** que aun no esta en `dev`, no lo abras todavia (o abrelo como borrador, `--draft`,
  y escribe "Depende de #N" en la descripcion). Se fusionan **en el orden de las dependencias**.

## 5. Revision

- El guardia de alcance y los tests deben estar en verde.
- Lo revisa **otra persona** (el duenio del repositorio y/o el duenio del area, segun `CODEOWNERS`).
  Nadie aprueba su propio PR.
- Si piden cambios, se hacen **en la misma rama** (commits nuevos y `git push`): el PR se actualiza solo.
- Mientras tu PR esta abierto, todo lo que empujes a esa rama **entra en ese PR**. No mezcles trabajo sin
  relacion: termina, fusiona y sigue.

## 6. Merge

Solo con el PR aprobado y los chequeos en verde, con **Create a merge commit** (no *Squash*).
Saltarse la revision (`--admin`) es solo para emergencias y se deja escrito en el PR.

## 7. Volver a actualizar

```bash
git fetch origin && git switch feat/<tu-servicio> && git merge origin/dev
```

Tu rama **no se borra**: sigues en ella. Las ramas `chore/` si se pueden borrar.

## Que revisa quien aprueba

- [ ] El titulo sigue el formato y la descripcion responde que, por que y como se probo.
- [ ] Todos los archivos estan dentro del area de quien lo envia (el guardia lo comprueba tambien).
- [ ] Tests y `ng build` en verde; sin secretos en el diff.
- [ ] Si depende de otro PR, ese ya esta en `dev`.
- [ ] El PR hace una sola cosa y es lo bastante pequeno para revisarse.

## Como se hace cumplir

| Regla | Quien la fuerza |
|---|---|
| Solo tu area | Guardia de alcance (GitHub Actions) y `CODEOWNERS` |
| Titulo con formato `tipo(alcance): ...` | Guardia de alcance |
| PR solo hacia `dev` (y `dev` a `main`) | Guardia de alcance |
| Aprobacion de otra persona y de los code owners | Proteccion de ramas |
| Rama al dia con `dev` antes de fusionar | Proteccion de ramas (`strict`) |
| Sin `push --force` ni borrar `dev`/`main` | Proteccion de ramas |
| El orden de pasos y el resto | Esta guia y la plantilla de PR |

## Para agentes de IA

Sigue estos 7 pasos **en este orden** y no te saltes ninguno. Antes de abrir el PR muestra la salida de
`git diff --name-only origin/dev...HEAD`. No fusiones un PR que no hayas abierto en esta tarea, y no uses
`--admin` salvo que el duenio del repositorio lo pida expresamente. Detalle de las reglas en [REGLAS_DE_ALCANCE.md](REGLAS_DE_ALCANCE.md).
