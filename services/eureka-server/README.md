# eureka-server

Servidor de descubrimiento (Netflix Eureka) de HelpDesk. Es la "guia telefonica": cada microservicio se registra
con su nombre (`servicio-tickets`, `servicio-identidad`...) y otros componentes lo encuentran por nombre en
lugar de por puerto.

Basado en el modulo `eureka-server` del proyecto de ejemplo `smart-parking`, sin cambios de logica.

| | |
|---|---|
| Puerto | `8081` |
| Panel | http://localhost:8081 |
| Registro (JSON) | `curl -H "Accept: application/json" localhost:8081/eureka/apps` |

## Como se usa

Los servicios **no** se registran por defecto, para que cada persona pueda levantar el suyo solo. Se activa con
variables de entorno (`docker-compose.yml` de la raiz ya lo hace):

```
EUREKA_ENABLED=true
EUREKA_URL=http://eureka-server:8081/eureka/
```

Levantarlo solo: `docker compose up -d eureka-server` desde la raiz, o `./mvnw spring-boot:run` con Java 21 o superior.
