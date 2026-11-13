# config-server

Servidor de configuracion (Spring Cloud Config) de HelpDesk. Entrega propiedades a los microservicios desde la
carpeta `src/main/resources/config-repo/` (modo `native`).

Basado en el modulo `config-server` del proyecto de ejemplo `smart-parking`, sin cambios de logica.

| | |
|---|---|
| Puerto | `8888` (el `8080` del ejemplo es de `servicio-tickets`) |
| Prueba | `curl localhost:8888/servicio-tickets/default` |

## Que va en `config-repo/`

- `application.properties`: propiedades compartidas por todos los servicios.
- `<nombre-del-servicio>.properties` (por ejemplo `servicio-tickets.properties`): propiedades de un solo servicio.

**Este repositorio es publico: nunca pongas contrasenas, claves ni tokens en `config-repo/`.** Los secretos van
por variables de entorno.

## Como se usa

Los servicios lo piden solo si se activa (por defecto no, para poder levantar un servicio solo):

```
CONFIG_ENABLED=true
CONFIG_URL=http://config-server:8888
```

Las variables de entorno de cada servicio tienen prioridad sobre lo que entregue el Config Server.
