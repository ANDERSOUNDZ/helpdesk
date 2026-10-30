# 📊 Servicio de Estadísticas (HelpDesk)

> **Desarrollador Responsable:** **Miguel Anderson Chanchay**  
> **Rama asignada:** `feat/estadisticas`  
> **Estado:** 🟢 **OPERATIVO EN DOCKER CON CRUD ANALÍTICO COMPLETO**

Microservicio de **analítica, auditoría y métricas de rendimiento en tiempo real** de la Mesa de Ayuda de TI.

---

## 🎯 ¿Qué hace este servicio? (Propósito de Negocio)
En un sistema de TI corporativo, las estadísticas no deben sobrecargar la base de datos principal de tickets. Este microservicio:
- **Mantiene contadores precalculados** de tickets abiertos, en progreso y resueltos.
- **Sirve métricas ultra-rápidas (en milisegundos)** para el panel de control (Dashboard gerencial).
- **Procesa transiciones de estado** (`TicketEstadoCambiado`) de forma desacoplada para reflejar la productividad del equipo de TI.
- **Permite auditoría y calibración manual** de métricas con verbos `GET`, `PUT` y `DELETE`.

---

## 🔌 Especificaciones de Red y Puertos
- **Aplicación Spring Boot:** Puerto `8082`
- **PostgreSQL Local:** Puerto `5436`
- **pgAdmin 4 (GUI Web):** Puerto `5052`
- **Base de Datos:** `estadisticas_db`
- **Usuario BD:** `estadisticas`
- **Contraseña BD:** `estadisticas`

---

## 📬 Endpoints Disponibles (Probados en Vivo)

| Método | Endpoint | Descripción | Ejemplo / Parámetros |
|:---:|---|---|---|
| **GET** | `/resumen` | Resumen ejecutivo con totales de tickets. | `GET http://localhost:8082/resumen` |
| **GET** | `/estadisticas/estados` | Lista todos los contadores de la base de datos. | `GET http://localhost:8082/estadisticas/estados` |
| **GET** | `/estadisticas/estados/{estado}` | Consulta el contador de un estado en específico. | `GET http://localhost:8082/estadisticas/estados/ABIERTO` |
| **PUT** | `/estadisticas/estados/{estado}` | Calibrar o ajustar manualmente un contador (Auditoría). | `PUT http://localhost:8082/estadisticas/estados/ABIERTO?cantidad=5` |
| **DELETE** | `/estadisticas/reset` | Resetea todos los contadores a cero para un nuevo ciclo. | `DELETE http://localhost:8082/estadisticas/reset` |
| **POST** | `/eventos/ticket-estado` | Recibe evento de cambio de ticket y recalcula en vivo. | Body: `{"estadoAnterior":"ABIERTO","estadoNuevo":"EN_PROGRESO"}` |

---

## 🚀 Cómo levantarlo con Docker

Ejecuta desde esta carpeta:
```bash
docker compose up -d --build
```

Verifica el estado de los 3 contenedores (`app`, `db`, `pgadmin`):
```bash
docker compose ps
```

---

## 🎯 ¿A DÓNDE DEBES IR Y QUÉ DEBES HACER AL LEVANTAR EL SERVICIO?

> ⚠️ **¡OJO CON EL ERROR 404 / WHITELABEL ERROR PAGE!**  
> Si abres en el navegador `http://localhost:8082/` directamente, verás:  
> `Whitelabel Error Page - There was an unexpected error (type=Not Found, status=404)`.  
> **Esto es completamente normal:** tu microservicio es una **API REST**, no sirve páginas web HTML en la raíz `/`.

### 1. Para probar tu API REST (Tus Endpoints):
Abre en tu navegador o en Postman el endpoint real:
👉 **[http://localhost:8082/resumen](http://localhost:8082/resumen)**  
*Respuesta esperada (JSON):* `{"abiertos":0,"enProgreso":0,"resueltos":0}`

O compruébalo por terminal:
```bash
curl.exe http://localhost:8082/resumen
```

---

### 2. Para ver tu Base de Datos en pgAdmin 4:
Abre en tu navegador:
👉 **[http://localhost:5052](http://localhost:5052)**
1. Inicia sesión con:
   - **Email:** `admin@admin.com`
   - **Password:** `admin`
2. Registra el servidor (**Servers** ➔ **Register** ➔ **Server...**):
   - **Pestaña General:** Nombre: `Estadisticas DB`
   - **Pestaña Connection:**
     - **Host:** `db` *(exactamente la palabra `db`)*
     - **Port:** `5432` *(puerto interno del contenedor)*
     - **Maintenance database:** `estadisticas_db`
     - **Username:** `estadisticas`
     - **Password:** `estadisticas`
     - Marca *Save password* y haz clic en **Save**.
3. En el árbol de la izquierda ve a: `Servers ➔ Estadisticas DB ➔ Databases ➔ estadisticas_db ➔ Schemas ➔ public ➔ Tables`.  
   ¡Verás tu tabla **`contadores_estado`** creada automáticamente!

---

### 3. Para ver el Tablero Gráfico en el Frontend (Angular Material):
Abre otra terminal en la carpeta `frontend/`:
```bash
cd ../../frontend
npm start
```
Y abre en tu navegador:  
👉 **[http://localhost:4200](http://localhost:4200)**  
*(Verás las tarjetas métricas consumiendo en vivo los datos de tu microservicio).*

---

## 🛑 Detener el servicio
```bash
docker compose down
```
*(No uses `-v` para preservar los datos de la base).*
