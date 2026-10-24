# ADR 0002: Servicios simples (entidades + controller)

- **Estado:** aceptada. Reemplaza la parte de "arquitectura Clean dentro de cada servicio" del [ADR 0001](0001-monorepo-clean-microservicios.md).
- **Fecha:** 2026-10-24

## Contexto

El ADR 0001 decidio que cada servicio usara capas Clean (`domain`, `application`, `infrastructure`,
`api`). En la practica eso agregaba muchas carpetas, puertos y mapeos para servicios que hoy solo tienen
una o dos entidades, y el equipo necesita primero algo que pueda levantar y probar con Postman.

## Decision

1. Cada servicio tiene solo `model/` (entidades) y `controller/` (endpoints), igual que `espacios-service`.
2. Los datos viven en memoria dentro del controller. No hay base de datos todavia.
3. Se mantiene todo lo que hace independientes a los servicios: monorepo con un proyecto Maven por
   servicio, referencias solo por id, clases propias para lo que se lee de otro servicio y ninguna
   llamada a otro servicio al crear algo propio.

## Consecuencias

- (+) Un servicio se entiende en minutos y cualquiera puede levantarlo y probarlo.
- (+) Menos codigo que mantener mientras el dominio es pequeno.
- (-) Reglas de negocio y acceso a datos conviven en el controller; no se pueden probar por separado.
- (-) Al agregar base de datos habra que separar capas. Es un cambio interno: la API y los demas
  servicios no se ven afectados. Se hara servicio por servicio, cuando su responsable lo necesite.
