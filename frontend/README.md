#  Frontend Web SPA (HelpDesk)

> **Desarrollador Responsable:** **Dylan Miguel Vargas Onofa**  
> **Framework:** Angular 19+ con **Angular Material Oficial** (Tema Azure/Material 3)  
> **Puerto de Desarrollo:** `http://localhost:4200`  
> **Rama asignada:** `feat/frontend`

---

##  ¿Qué hace este Frontend? (Propósito de Negocio)
Es la interfaz web para los usuarios y técnicos de la Mesa de Ayuda de TI. Permite:
1. **Ver métricas en tiempo real:** Tarjetas superiores con tickets totales, abiertos, en progreso y resueltos (consume tu `servicio-estadisticas`).
2. **Registrar incidentes de TI:** Formulario con título, descripción y prioridad (`BAJA`, `MEDIA`, `ALTA`).
3. **Bandeja de incidentes:** Tabla de tickets con acciones directas para **Asignar** y **Resolver** casos.
4. **Diseño Estándar Angular Material:** Implementado con componentes oficiales (`mat-toolbar`, `mat-card`, `mat-table`, `mat-form-field`, `mat-chip`, `mat-snack-bar`), **100% responsive** para móviles, tablets y escritorio.

---

##  Cómo levantarlo en desarrollo

1. Abre una terminal en la carpeta `frontend/`:
   ```bash
   cd frontend
   npm start
   ```
2. Abre tu navegador en: **`http://localhost:4200`**

>  **Nota sobre el Proxy:** `npm start` arranca automáticamente con `--proxy-config proxy.conf.json`. Esto redirige de forma transparente las llamadas hacia los microservicios sin problemas de CORS:
> - `/resumen`  `http://localhost:8082` (Estadísticas - Miguel)
> - `/tickets`  `http://localhost:8080` (Tickets - Ariel)
> - `/usuarios`  `http://localhost:8083` (Identidad)
> - `/comentarios`  `http://localhost:8084` (Comunicación - Gabriel)

---

##  Moldeabilidad para el API Gateway (A futuro)
El frontend está diseñado para no tocar una sola línea de lógica cuando Dylan active el API Gateway en el puerto `:8000`:
- La configuración reside únicamente en [`src/environments/environment.ts`](src/environments/environment.ts).
- Al pasar a producción con Gateway, solo se ajusta la URL base a `http://localhost:8000/api` y todo seguirá funcionando sin modificaciones.

---

## ️ Roadmap para Dylan (Continuación del Frontend)
- [x] **Fase Base UI:** Proyecto Angular configurado con Angular Material estándar y responsive.
- [x] **Consumo desacoplado:** Configuración con proxy local y environments listos.
- [ ] **Chat de Soporte (Gabriel):** Crear una vista modal (`MatDialog`) para ver y agregar comentarios por ticket (`/comentarios`).
- [ ] **Campana de Notificaciones:** Agregar icono con `matBadge` en el toolbar para mostrar avisos al usuario (`/notificaciones`).
- [ ] **Integración con Gateway:** Conectar todo al puerto `:8000` cuando esté levantado el Gateway.
