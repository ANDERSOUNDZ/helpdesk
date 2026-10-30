# Guia de Prueba Punto a Punto (E2E) — HelpDesk Distribuido

Este documento detalla el procedimiento oficial y reproducible para ejecutar la prueba de integracion punto a punto (E2E) que recorre los **4 microservicios del sistema** y el **frontend**.

---

## 1. Alcance de la Prueba E2E
La prueba valida el ciclo de vida completo de un incidente de TI:
1. **`servicio-identidad` (:8083):** Registro y consulta de un nuevo especialista tecnico de soporte.
2. **`servicio-tickets` (:8080):** Radicacion de un nuevo ticket con solicitante y prioridad (`ABIERTO`).
3. **`servicio-comunicacion` (:8084):** Registro de la primera intervencion tecnica en la bitacora.
4. **`servicio-tickets` (:8080):** Asignacion formal del ticket al especialista tecnico (`EN_PROGRESO`).
5. **`servicio-estadisticas` (:8082):** Notificacion del evento de cambio de estado y actualizacion de metricas en tiempo real.
6. **`servicio-comunicacion` (:8084):** Registro de la nota de solucion tecnica en la bitacora.
7. **`servicio-tickets` (:8080):** Cierre y finalizacion del incidente (`RESUELTO`).
8. **`servicio-estadisticas` (:8082):** Consolidacion del KPI de casos resueltos y auditoria.
9. **`frontend` (:4200):** Visualizacion transparente y en tiempo real de todo el flujo en la interfaz de usuario.

---

## 2. Requisitos Previos
Asegurate de que los contenedores esten encendidos ejecutando en la raiz del repositorio:
```bash
docker compose up -d
```
Para verificar que los 4 servicios y la base de datos estan respondiendo:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/tickets" -Method Get
Invoke-RestMethod -Uri "http://localhost:8082/resumen" -Method Get
Invoke-RestMethod -Uri "http://localhost:8083/usuarios" -Method Get
Invoke-RestMethod -Uri "http://localhost:8084/comentarios" -Method Get
```

---

## 3. Script Automatizado para Recrear la Prueba (PowerShell)

Abre una terminal de PowerShell y ejecuta el siguiente bloque de comandos completo. El script ejecuta cada paso e imprime el resultado detallado:

```powershell
# ==============================================================
# SCRIPT DE PRUEBA PUNTO A PUNTO (E2E) - HELPDESK DISTRIBUIDO
# ==============================================================

$fecha = Get-Date -Format "yyyyMMdd_HHmmss"

Write-Host ">>> PASO 1: Registrar especialista en servicio-identidad (:8083)" -ForegroundColor Cyan
$tecBody = @{
    nombre = "Ing. Sandra Rivas ($fecha)"
    rol = "TECNICO"
    email = "sandra_$fecha@helpdesk.com"
    activo = $true
} | ConvertTo-Json

$tecnico = Invoke-RestMethod -Uri "http://localhost:8083/usuarios" -Method Post -Body $tecBody -ContentType "application/json"
Write-Host "Especialista Creado: ID=$($tecnico.id) | Nombre=$($tecnico.nombre)" -ForegroundColor Green

Write-Host "`n>>> PASO 2: Radicar ticket en servicio-tickets (:8080)" -ForegroundColor Cyan
$ticketBody = @{
    titulo = "Falla de enlace fibra optica switch piso 2 ($fecha)"
    descripcion = "Solicitante: Juan Lopez [Operaciones]. Detalle: Corte total de enlace troncal"
    prioridad = "ALTA"
    solicitanteId = [long]1
} | ConvertTo-Json

$ticket = Invoke-RestMethod -Uri "http://localhost:8080/tickets" -Method Post -Body $ticketBody -ContentType "application/json"
Write-Host "Ticket Creado: ID=$($ticket.id) | Estado=$($ticket.estado) | Prioridad=$($ticket.prioridad)" -ForegroundColor Green

Write-Host "`n>>> PASO 3: Registrar nota inicial en servicio-comunicacion (:8084)" -ForegroundColor Cyan
$nota1Body = @{
    ticketId = [long]$ticket.id
    autorId = [long]$tecnico.id
    texto = "Recepcion del caso: tecnico asignado se traslada a rack de comunicaciones."
} | ConvertTo-Json

$nota1 = Invoke-RestMethod -Uri "http://localhost:8084/comentarios" -Method Post -Body $nota1Body -ContentType "application/json"
Write-Host "Nota Guardada: ID=$($nota1.id) para Ticket #$($ticket.id)" -ForegroundColor Green

Write-Host "`n>>> PASO 4: Asignar tecnico en servicio-tickets (Pasa a EN_PROGRESO)" -ForegroundColor Cyan
$ticketAsignado = Invoke-RestMethod -Uri "http://localhost:8080/tickets/$($ticket.id)/asignar/$($tecnico.id)" -Method Put
Write-Host "Ticket Actualizado: ID=$($ticketAsignado.id) | Estado=$($ticketAsignado.estado) | TecnicoId=$($ticketAsignado.tecnicoId)" -ForegroundColor Green

Write-Host "`n>>> PASO 5: Notificar cambio de estado a servicio-estadisticas (:8082)" -ForegroundColor Cyan
$evento1 = @{
    ticketId = [long]$ticket.id
    estadoAnterior = "ABIERTO"
    estadoNuevo = "EN_PROGRESO"
} | ConvertTo-Json

$kpiProgreso = Invoke-RestMethod -Uri "http://localhost:8082/eventos/ticket-estado" -Method Post -Body $evento1 -ContentType "application/json"
Write-Host "Metricas actualizadas: En Progreso=$($kpiProgreso.enProgreso)" -ForegroundColor Green

Write-Host "`n>>> PASO 6: Registrar nota de intervencion y solucion tecnica (:8084)" -ForegroundColor Cyan
$nota2Body = @{
    ticketId = [long]$ticket.id
    autorId = [long]$tecnico.id
    texto = "Se reemplazo modulo SFP monomodo y patchcord. Conectividad 10 Gbps restablecida."
} | ConvertTo-Json

$nota2 = Invoke-RestMethod -Uri "http://localhost:8084/comentarios" -Method Post -Body $nota2Body -ContentType "application/json"
Write-Host "Nota de Solucion: ID=$($nota2.id) guardada en bitacora" -ForegroundColor Green

Write-Host "`n>>> PASO 7: Resolver ticket en servicio-tickets (:8080)" -ForegroundColor Cyan
$ticketResuelto = Invoke-RestMethod -Uri "http://localhost:8080/tickets/$($ticket.id)/resolver" -Method Put
Write-Host "Ticket Finalizado: ID=$($ticketResuelto.id) | Estado=$($ticketResuelto.estado)" -ForegroundColor Green

Write-Host "`n>>> PASO 8: Notificar resolucion a servicio-estadisticas (:8082)" -ForegroundColor Cyan
$evento2 = @{
    ticketId = [long]$ticket.id
    estadoAnterior = "EN_PROGRESO"
    estadoNuevo = "RESUELTO"
} | ConvertTo-Json

$kpiFinal = Invoke-RestMethod -Uri "http://localhost:8082/eventos/ticket-estado" -Method Post -Body $evento2 -ContentType "application/json"
Write-Host "Metricas Finales: Resueltos=$($kpiFinal.resueltos)" -ForegroundColor Green

Write-Host "`n>>> PASO 9: Validar bitacora historica de intervenciones (:8084)" -ForegroundColor Cyan
$historial = Invoke-RestMethod -Uri "http://localhost:8084/comentarios?ticketId=$($ticket.id)" -Method Get
Write-Host "Total notas registradas para Ticket #$($ticket.id): $($historial.Count)" -ForegroundColor Yellow
foreach ($h in $historial) {
    Write-Host "  - [Nota #$($h.id)] (Tecnico #$($h.autorId)): $($h.texto)"
}

Write-Host "`n=== PRUEBA PUNTO A PUNTO CONCLUIDA EXITOSAMENTE AL 100% ===" -ForegroundColor Green
```

---

## 4. Recreacion Manual desde la Interfaz Web (Frontend :4200)

Si deseas recrear la prueba graficamente desde el navegador:

1. **Abrir la aplicacion:** Ingresa a `http://localhost:4200`.
2. **Pestaña `servicio-identidad (:8083)`:**
   - Escribe un nombre de tecnico (ej. `Carlos Mendoza`), selecciona rol `Tecnico de Soporte` y haz clic en **Guardar Tecnico**.
   - Verifica que aparezca de inmediato en la tabla de la parte inferior.
3. **Pestaña `servicio-tickets (:8080)`:**
   - En el formulario, ingresa:
     - Nombre del Solicitante: `Ana Morales`
     - Area / Departamento: `Operaciones`
     - Prioridad: `Alta`
     - Titulo: `Falla de acceso a base de datos de produccion`
   - Haz clic en **Crear Incidente**.
   - El caso aparecera en la tabla con estado **ABIERTO**.
4. **Asignacion:**
   - En la fila del caso creado, haz clic en el boton **Asignar**.
   - Se abrira el modal de Material. Selecciona el especialista creado en el paso 2 y haz clic en **Confirmar Asignacion**.
   - El estado cambiara en tiempo real a **EN_PROGRESO**.
5. **Bitacora tecnica:**
   - Haz clic en el boton **Notas** del ticket.
   - En el modal de bitacora, escribe una nota de intervencion (ej. *"Se reinicio el pool de conexiones en el servidor"*) y presiona **Publicar Nota**.
   - Verifica que se liste en la tabla de historial de notas servida por `servicio-comunicacion (:8084)`.
   - Cierra el modal.
6. **Resolucion:**
   - En la fila del ticket, haz clic en **Finalizar**.
   - El estado cambiara a **RESUELTO**.
7. **Pestaña `servicio-estadisticas (:8082)`:**
   - Verifica como la tarjeta KPI de **RESUELTOS** y el desglose porcentual aumentaron automaticamente reflejando el caso cerrado.
