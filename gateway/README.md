# 🌐 API Gateway (HelpDesk) — [STANDBY]

> **Desarrollador Responsable:** **Dylan Miguel Vargas Onofa**  
> **Estado:** ⏳ **STANDBY / LISTO PARA INICIAR**  
> **Puerto asignado:** `8000`  
> **Rama asignada:** `feat/gateway`

---

## 🎯 Objetivo del Componente
El **API Gateway** es la puerta de enlace única y transparente hacia todo el backend distribuido de la Mesa de Ayuda de TI. Permite que el cliente web (Frontend) se comunique con una sola dirección (`http://localhost:8000/api/...`) sin tener que conocer los puertos internos de cada microservicio.

---

## 📋 Tareas en Standby para Dylan
Cuando inicies el desarrollo de este módulo con el asistente, aquí está el punto de partida:

1. **Creación del Proyecto:**
   - Crear una aplicación Spring Boot con **Spring Cloud Gateway** y dependencias de Reactive Gateway.
2. **Configuración de Rutas (`application.yml`):**
   - Mapear las rutas hacia los 4 microservicios backend:
     - `/api/tickets/**` ➔ `http://localhost:8080/tickets/**` (Ariel)
     - `/api/usuarios/**` ➔ `http://localhost:8083/usuarios/**` (Identidad)
     - `/api/comentarios/**` ➔ `http://localhost:8084/comentarios/**` (Gabriel)
     - `/api/notificaciones/**` ➔ `http://localhost:8084/notificaciones/**` (Gabriel)
     - `/api/estadisticas/**` ➔ `http://localhost:8082/resumen` (Miguel)
3. **CORS Global:**
   - Habilitar CORS en el Gateway para permitir peticiones desde `http://localhost:3000` (Frontend) con métodos `GET, POST, PUT, DELETE, OPTIONS`.
4. **Dockerización:**
   - Incluir `Dockerfile` y agregar el servicio en el `docker-compose.yml` de la raíz en puerto `8000`.

---

## 🛑 Estado Actual
Este módulo se encuentra en **Standby estructurado**. Los microservicios de **Ariel, Gabriel y Miguel** ya tienen sus contratos REST y puertos listos para ser enrutados.
