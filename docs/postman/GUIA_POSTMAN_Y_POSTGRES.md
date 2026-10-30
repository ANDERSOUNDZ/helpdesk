#  Guía de Postman y Gestión de PostgreSQL (HelpDesk)

---

## 1.  Uso de Postman (Desktop App vs Extensión en IDE)

La colección oficial del proyecto se encuentra en:  
 [`docs/postman/HelpDesk_API_Coleccion.json`](HelpDesk_API_Coleccion.json)

Es **100% compatible y funciona exactamente igual** en ambas modalidades:

### A. En Postman Desktop App:
1. Abre Postman Desktop.
2. Clic en **Import** (arriba a la izquierda).
3. Selecciona el archivo `docs/postman/HelpDesk_API_Coleccion.json`.
4. Todos los endpoints (`GET`, `POST`, `PUT`, `DELETE`) quedan listos con sus URLs y cuerpos JSON preconfigurados.

### B. En el IDE (Extensión Postman / Thunder Client en VS Code o Antigravity):
1. Abre la extensión en la barra lateral izquierda del IDE.
2. Ve a **Collections**  Clic en `...` o en el botón de **Import**.
3. Selecciona `docs/postman/HelpDesk_API_Coleccion.json`.
4. Puedes ejecutar peticiones haciendo clic en **Send** directamente desde tu editor sin cambiar de ventana.

---

## 2.  Cómo Levantar PostgreSQL

El proyecto ofrece dos opciones para levantar PostgreSQL: **modo individual (por servicio)** o **modo conjunto (todos los servicios)**.

###  Opción A: Levantar SOLO PostgreSQL de tu servicio (`servicio-estadisticas`)
Si estás trabajando únicamente en tu microservicio:

```bash
cd services/servicio-estadisticas
docker compose up -d db pgadmin
```
*(Si también quieres levantar tu app de Spring Boot, ejecuta simplemente `docker compose up -d --build`).*

####  IMPORTANTE: Cómo registrar el Servidor en pgAdmin sin errores de conexión
> ️ **ERROR COMÚN:** Si pones `localhost`, `127.0.0.1` o el puerto `5436` dentro de pgAdmin, obtendrás el error:  
> `connection to server at "127.0.0.1", port 5436 failed: Connection refused`.  
> Esto ocurre porque para pgAdmin (que corre en un contenedor), `localhost` es él mismo y no tu máquina host.

**Configuración EXACTA que debes poner en pgAdmin (Pestaña Connection):**
- **Host name/address:** `db`  *(Pon la palabra literal `db`, NO pongas localhost ni 127.0.0.1)*
- **Port:** `5432`  *(Pon `5432` interno, NO pongas 5436)*
- **Maintenance database:** `estadisticas_db`
- **Username:** `estadisticas`
- **Password:** `estadisticas`
- Marca *Save password* y clic en **Save**.

---

###  Opción B: Levantar PostgreSQL General (Para TODO el proyecto)
Si quieres levantar la base de datos central que contiene las bases de los 4 microservicios:

```bash
# Desde la carpeta raíz del proyecto:
docker compose up -d db pgadmin
```

#### Configuración en pgAdmin General ([http://localhost:5055](http://localhost:5055)):
- **Host name/address:** `db`
- **Port:** `5432`
- **Maintenance database:** `postgres`
- **Username:** `postgres`
- **Password:** `postgres`

---

##  Tabla Maestra de Conexión pgAdmin para cada Desarrollador

| Desarrollador | Microservicio | URL de pgAdmin | Host en pgAdmin | Puerto en pgAdmin | Maintenance DB | Usuario / Clave |
| :--- | :--- | :---: | :---: | :---: | :--- | :--- |
| **Ariel Guerrero** | `servicio-tickets` | [localhost:5050](http://localhost:5050) | `db` | `5432` | `tickets_db` | `tickets` / `tickets` |
| **Miguel Chanchay** | `servicio-estadisticas` | [localhost:5052](http://localhost:5052) | `db` | `5432` | `estadisticas_db` | `estadisticas` / `estadisticas` |
| **Equipo** | `servicio-identidad` | [localhost:5053](http://localhost:5053) | `db` | `5432` | `identidad_db` | `identidad` / `identidad` |
| **Gabriel Ponce** | `servicio-comunicacion` | [localhost:5054](http://localhost:5054) | `db` | `5432` | `comunicacion_db` | `comunicacion` / `comunicacion` |
| **Todo el Sistema** | Global (Raíz) | [localhost:5055](http://localhost:5055) | `db` | `5432` | `postgres` | `postgres` / `postgres` |

*Las credenciales para entrar al login web de pgAdmin en todos los casos son: Email `admin@admin.com` \| Contraseña `admin`.*

---

##  Detener PostgreSQL sin perder datos
En cualquiera de los dos casos, ejecuta:
```bash
docker compose down
```
> ️ **Regla de Oro:** NUNCA uses el flag `-v` (`docker compose down -v`), ya que ese flag borra los volúmenes de disco y destruiría los datos guardados en PostgreSQL.
