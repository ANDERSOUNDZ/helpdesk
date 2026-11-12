# Exponer el frontend con un tunel de Cloudflare

Guia para publicar HelpDesk en internet de forma temporal (demos, pruebas con companeros) usando un
**tunel rapido** de Cloudflare (`trycloudflare.com`). No requiere cuenta ni dominio.

## Como funciona

Se expone **un solo puerto: el del frontend (4200)**. El servidor de desarrollo de Angular reenvia las
llamadas (`/tickets`, `/usuarios`, `/comentarios`, `/notificaciones`, `/resumen`, `/estadisticas`) a los
microservicios que corren en tu PC, segun `frontend/proxy.conf.json`. Por eso no hace falta un tunel por
servicio ni un gateway.

```
Visitante --> https://<algo>.trycloudflare.com --> cloudflared (tu PC) --> ng serve :4200 --> proxy --> 8080 / 8082 / 8083 / 8084
```

## Requisitos

- Docker Desktop abierto y los servicios levantados.
- Node/npm con las dependencias del frontend instaladas (`npm install` en `frontend/`).
- `cloudflared` instalado. Comprobar con:

```bash
cloudflared --version
```

Si no esta instalado: `winget install --id Cloudflare.cloudflared` (Windows) y abrir una terminal nueva.

## Pasos

Se usan **tres terminales**. El orden importa: primero el backend, luego el frontend y al final el tunel.

### 1. Levantar los microservicios (terminal 1, raiz del repo)

```bash
docker compose up -d --build
```

Comprobar que responden:

```bash
curl.exe -s localhost:8080/tickets
curl.exe -s localhost:8082/resumen
curl.exe -s localhost:8083/usuarios
curl.exe -s localhost:8084/comentarios
```

### 2. Levantar el frontend (terminal 2, carpeta `frontend/`)

```bash
npm start
```

Esto ejecuta `ng serve --proxy-config proxy.conf.json` y deja la app en `http://localhost:4200`.
Comprobar en el navegador que carga y que la lista de tickets responde **antes** de abrir el tunel.

`angular.json` ya permite el dominio `.trycloudflare.com` en `allowedHosts`; sin eso Angular responde
`Blocked request. This host is not allowed`.

### 3. Abrir el tunel (terminal 3, cualquier carpeta)

```bash
cloudflared tunnel --url http://localhost:4200
```

En la salida aparece un recuadro con la URL publica, parecida a:

```
https://palabra-palabra-palabra.trycloudflare.com
```

Esa es la direccion que se comparte. Abrirla tambien desde el celular o con datos moviles para confirmar que
funciona desde fuera de tu red.

### 4. Cerrar el tunel

`Ctrl + C` en la terminal 3. La URL deja de funcionar al instante.

## Cosas a tener en cuenta

- **La URL cambia cada vez** que se reinicia `cloudflared`. Para una URL fija se necesita una cuenta de
  Cloudflare y un tunel con nombre (no esta configurado en este proyecto).
- **Tu PC debe seguir encendida** con las tres terminales abiertas; si se cierra una, la app deja de responder.
- **No hay autenticacion.** Cualquiera con la URL puede crear, editar y borrar datos (por ejemplo
  `DELETE /tickets/{id}` y `DELETE /estadisticas/reset`). Compartir la URL solo con quien corresponda y
  cerrar el tunel al terminar.
- **Exponer unicamente el 4200.** No abrir tuneles a 8080-8084 ni a los puertos de PostgreSQL (5432-5436).
- Los datos que se creen quedan en las bases locales (volumen de Docker). Apagar con `docker compose down`,
  **nunca** `down -v` salvo que se quiera borrar todo.

## Si algo falla

| Sintoma | Causa probable | Que hacer |
|---|---|---|
| `Blocked request. This host is not allowed` | Angular no reconoce el dominio del tunel | Confirmar que `frontend/angular.json` tiene `allowedHosts` con `.trycloudflare.com` y reiniciar `npm start` |
| `cloudflared` no se reconoce como comando | No esta en el `PATH` | Abrir una terminal nueva o instalarlo con `winget` |
| La pagina carga pero las listas salen vacias o dan error | Los microservicios no estan arriba, o el proxy no llego | Repetir las comprobaciones del paso 1 y revisar `docker compose ps` |
| Error 1033 / 502 de Cloudflare | `ng serve` no esta corriendo en el 4200 | Levantar el frontend (paso 2) y reintentar |
| El tunel no conecta o tarda mucho | Red que bloquea el trafico saliente de `cloudflared` | Probar con otra red (por ejemplo, datos del celular) |
