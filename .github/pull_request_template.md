## Que cambia

<!-- Una o dos lineas: que hace este PR y por que. -->

## Mi area y tipo de PR

- [ ] Rama `feat/<servicio>`: solo toca **mi area** (mi servicio y mi pestana del frontend)
- [ ] Rama `chore/<tema>`: solo toca **archivos compartidos** (proxy, services/models del frontend, docker-compose, docs...), con cambios aditivos
- [ ] Mi servicio: <!-- tickets / comunicacion / identidad / estadisticas / frontend / gateway -->

## Checklist

- [ ] El PR va hacia `dev` (nunca hacia `main`) y uso *Create a merge commit*
- [ ] Hice `git merge origin/dev` antes de abrir el PR
- [ ] Los commits siguen Conventional Commits (`feat(tickets): ...`)
- [ ] Probado localmente (tests del servicio en verde; `ng build` si toque el frontend)
- [ ] Revise `git diff --name-only origin/dev...HEAD` y **todos** los archivos son de mi area (docs/REGLAS_DE_ALCANCE.md)
- [ ] Si mi funcionalidad necesita cambios en el codigo de otro duenio, **no los hice**: los anote abajo
- [ ] Sin secretos ni archivos `.env` en el diff

## Pendiente de integracion por otros duenios

<!-- Si algo de lo tuyo necesita cambios en pantallas o servicios ajenos, listalo aqui con el duenio. Si no hay, escribe "Ninguno". -->

## Archivos que cambio

<!-- Pega la salida de: git diff --name-only origin/dev...HEAD -->


## Notas para quien revisa

<!-- Decisiones de diseno, riesgos, capturas, como probarlo. -->
