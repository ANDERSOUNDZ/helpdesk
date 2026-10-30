# 📋 Bitácora de Eventos y Auditoría de Despliegue Local (Docker)

> **Microservicio:** `servicio-estadisticas`  
> **Responsable:** Miguel Anderson Chanchay  
> **Fecha:** 2026-10-29 (Sesión de prueba en vivo)  
> **Estado:** 🟢 **EXITOSO — CERO ERRORES**

---

## ⏱️ 1. Cronología de Eventos al Levantar el Servicio

| Minuto / Paso | Evento Observado | Resultado |
|---|---|:---:|
| **Paso 1** | Descarga de imágenes base (`postgres:17-alpine`, `dpage/pgadmin4:latest`, `maven:3.9-eclipse-temurin-21-alpine`). | ✅ OK |
| **Paso 2** | Creación de la red aislada `servicio-estadisticas_default` y el volumen persistente `servicio-estadisticas_datos`. | ✅ OK |
| **Paso 3** | Arranque del contenedor de base de datos (`servicio-estadisticas-db-1`) en puerto local `5436`. | ✅ OK |
| **Paso 4** | Ejecución del Healthcheck (`pg_isready -U estadisticas -d estadisticas_db`). Pasó a estado **healthy** en 3 segundos. | ✅ OK |
| **Paso 5** | Compilación multietapa de la aplicación Spring Boot dentro del Dockerfile (Maven package con Java 21) en 3.6 segundos. | ✅ OK |
| **Paso 6** | Arranque del contenedor Spring Boot (`servicio-estadisticas-app-1`) y conexión del pool Hikari (`jdbc:postgresql://db:5432/estadisticas_db`). | ✅ OK |
| **Paso 7** | Hibernate DDL Auto: Creación automática de la tabla `public.contadores_estado` en PostgreSQL. | ✅ OK |
| **Paso 8** | Inicialización del servidor Tomcat en puerto `8082` en 4.19 segundos. | ✅ OK |
| **Paso 9** | Prueba de humo HTTP `GET http://localhost:8082/resumen` devolvió `{"abiertos":0,"enProgreso":0,"resueltos":0}` con código HTTP 200. | ✅ OK |

---

## 🔍 2. Análisis Preventivo: ¿Qué podría pasar y cómo resolverlo?

Se analizaron posibles fallos que un desarrollador nuevo podría enfrentar en su máquina y se documentan aquí las soluciones preventivas:

### ⚠️ Caso A: Docker Desktop no está encendido
- **Síntoma:** Error `Cannot connect to the Docker daemon`.
- **Causa:** El servicio de Docker no está corriendo en el sistema operativo.
- **Solución:** Abrir Docker Desktop y esperar a que el ícono esté en verde.

### ⚠️ Caso B: Conflicto de Puertos
- **Síntoma:** Error `Bind for 0.0.0.0:8082 failed: port is already allocated` o `5436 already in use`.
- **Causa:** El desarrollador tiene una instancia previa de Spring Boot o Postgres corriendo fuera de Docker en el mismo puerto.
- **Solución:** Verificar qué proceso usa el puerto con `netstat -ano | findstr :8082` o detenerlo antes de ejecutar `docker compose up`.

### ⚠️ Caso C: Borrado accidental de datos
- **Regla Inmutable:** Nunca ejecutar `docker compose down -v`.
- **Consecuencia de violar la regla:** El flag `-v` destruye el volumen de PostgreSQL y borra todas las estadísticas y tablas.
- **Acción preventiva:** Usar siempre `docker compose down` (sin `-v`) para pausar los contenedores manteniendo intactos todos los registros.

### ⚠️ Caso D: Latencia en la primera descarga
- **Nota:** La primera vez que un desarrollador levanta el contenedor, Docker descarga las imágenes oficiales (~500 MB en total). Esto tarda de 1 a 3 minutos dependiendo de la velocidad de Internet. Las siguientes veces levantará en menos de **5 segundos**.

---

## 🐘 3. Verificación de Tablas en PostgreSQL
Se inspeccionó directamente la base de datos dentro del contenedor:
```sql
psql -U estadisticas -d estadisticas_db
\dt
```
**Resultado:**
- Tabla `contadores_estado` creada automáticamente con columnas `estado` (PK) y `cantidad` (integer).
