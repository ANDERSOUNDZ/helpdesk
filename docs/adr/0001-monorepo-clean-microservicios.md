# ADR 0001: Monorepo, microservicios y arquitectura Clean

- **Estado:** aceptada
- **Fecha:** 2026-10-23

## Contexto

Equipo de 5 personas construyendo un HelpDesk. La meta es una arquitectura de
microservicios escalable y mantenible, empezando solo por el backend. El proyecto de
referencia (`espacios-service`) es un servicio unico en capas simples
(controller / service / model), sin separar dominio de persistencia.

## Decision

1. **Monorepo.** Un solo repositorio con una carpeta por servicio. Un solo lugar para el
   CI, las reglas de equipo y la documentacion; los servicios siguen siendo independientes.
2. **Microservicios desde el inicio.** Cada servicio es un modulo Maven y una imagen Docker
   propios, con su base de datos propia. No se comparte codigo de dominio entre servicios.
3. **Clean / hexagonal dentro de cada servicio.** Capas `domain`, `application`,
   `infrastructure` y `api`. Las dependencias solo apuntan hacia el dominio.
4. **Docker como entorno unico** para desarrollo y CI, de modo que Windows, macOS y Linux
   den el mismo resultado.
5. **Sin API gateway por ahora.** Se agregara cuando haya mas de un servicio expuesto.

## Consecuencias

- (+) Un PR puede tocar varios servicios y se revisa en un solo lugar.
- (+) Separar un servicio a su propio repo mas adelante es barato: ya es independiente.
- (-) El CI debe filtrar por ruta para no construir todo en cada PR.
- (-) Sin una libreria de dominio compartida habra algo de duplicacion de DTOs entre servicios.
  Se acepta a cambio de no acoplarlos.
