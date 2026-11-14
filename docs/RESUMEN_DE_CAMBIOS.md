# Resumen de cambios: del trabajo de Gabriel a hoy

**Periodo:** 12 al 14 de noviembre de 2026.
**Para quién es:** cualquier persona del equipo o del jurado, sin necesidad de saber programar.

---

## En una frase

Partimos de un sistema donde Gabriel acababa de terminar su parte (comentarios, adjuntos y encuestas) y lo convertimos en un sistema **ordenado, protegido con usuarios y contraseñas, con roles, y mejor organizado para que el equipo no se pise entre sí**.

## Los números

| Qué | Cantidad |
|---|---|
| Cambios aceptados en el repositorio | **16** |
| Archivos que se tocaron | **98** |
| Líneas nuevas agregadas | **más de 5.500** |
| Pruebas automáticas que revisan que todo funciona | **83** (y todas pasan) |
| Roles de usuario nuevos | **3** (solicitante, técnico, administrador) |

---

## Qué pasó, día por día

| Día | Qué se hizo |
|---|---|
| **12 de noviembre** | Entra el trabajo de Gabriel. Se crean las reglas para que cada quien trabaje solo en lo suyo. |
| **13 de noviembre** | Se agrega la "guía telefónica" de servicios, el chequeo de salud, el login con roles y las pantallas de acceso. |
| **14 de noviembre** | Se ordena la documentación y se mueven las reglas al documento principal del proyecto. |

---

## Los cambios, explicados fácil

### 1. El trabajo de Gabriel: comunicación completa
**Qué es:** la parte del sistema donde las personas se hablan sobre un problema.

- **Comentarios con archivos:** ahora se puede adjuntar una foto o un PDF a un comentario (hasta 3 archivos de 5 MB).
- **Avisos:** cuando pasa algo en un ticket (cambia de estado, alguien comenta), la persona recibe un aviso. Hay un contador de avisos sin leer y un botón "marcar todos como leídos".
- **Encuesta de satisfacción:** cuando se resuelve un problema, se le pide a quien lo reportó que califique la atención del 1 al 5. Se puede ver el promedio.

**Por qué importa:** el soporte técnico no es solo arreglar; también es comunicarse y medir si el usuario quedó contento.

### 2. Reglas para que nadie se pise con el trabajo de otro
**El problema:** el trabajo de Gabriel traía cambios en partes que no eran suyas. No chocó con nada, pero estuvo cerca.

**La solución, en tres capas:**
1. **Cada persona tiene su área.** Ariel, Gabriel, Anthony, Dylan y Miguel tienen una zona asignada y solo pueden cambiar esa. Está escrito en el documento principal (`ORQUESTADOR_MAESTRO.md`).
2. **Un "vigilante" automático.** Cuando alguien propone un cambio, GitHub revisa solo que no toque archivos ajenos. Si lo hace, **lo rechaza**.
3. **Candados en las ramas principales.** Nadie puede meter cambios directo: hace falta que el dueño del proyecto los apruebe.

**También se fijó un orden de trabajo para todos:** actualizar, hacer cambios pequeños, subirlos, pedir revisión, aprobar y volver a actualizar. Los títulos de los cambios deben seguir un formato claro.

### 3. Una "guía telefónica" de servicios y un chequeo de salud
Tomamos como modelo el proyecto de ejemplo *smart-parking*.

- **Eureka (la guía telefónica):** cada parte del sistema se anota con su nombre al encender. Así se pueden encontrar entre sí sin saber el "número" exacto.
- **Config Server (el tablero de ajustes):** un lugar central para guardar configuraciones.
- **Chequeo de salud:** cada servicio responde "estoy bien" o "estoy mal". Docker lo usa para saber cuándo algo está realmente listo, y Eureka muestra la salud verdadera.

**Detalle importante:** todo esto es opcional. Si alguien quiere trabajar solo en su parte, sigue pudiendo hacerlo sin encender nada de esto.

### 4. Login con roles: la novedad más visible
**Antes:** cualquiera que abriera la aplicación podía crear, cambiar o borrar cualquier cosa.
**Ahora:** hay una pantalla de acceso con usuario y contraseña, y cada persona ve y hace solo lo que le corresponde.

| Rol | Quién es | Qué puede hacer |
|---|---|---|
| **Solicitante** | Quien reporta un problema | Crear tickets, ver su avance, comentar, reabrir y calificar |
| **Técnico** | Quien lo atiende | Todo lo anterior, más **finalizar** tickets y ver las **estadísticas** |
| **Administrador** | Quien supervisa | Todo, más **asignar** técnicos, **editar y borrar** tickets, y **crear usuarios** |

**Cómo funciona, en simple:** al entrar con usuario y contraseña, el sistema entrega un "pase digital" que vale **60 minutos**. Cada parte del sistema revisa ese pase antes de hacer algo. Si el pase es falso o venció, la persona vuelve a la pantalla de acceso.

**Cosas que cuidamos:**
- Las contraseñas se guardan **cifradas**: ni siquiera el administrador puede verlas.
- Las claves secretas **no se suben al repositorio**, porque es público: van en un archivo local.
- **Viene apagado por defecto.** Así nadie del equipo tiene que cambiar su forma de trabajar hasta que quiera encenderlo.
- Los usuarios que ya existían se conservaron. El rol viejo "estudiante" pasó a "solicitante" automáticamente.

**Cómo se ve:** hay capturas en [`docs/AUTENTICACION.md`](AUTENTICACION.md) (login, vista del solicitante y vista del administrador).

### 5. Documentación al día
- **README** y **orquestador** actualizados con el estado real (antes tenían errores, enlaces rotos y dos numeraciones de fases distintas).
- **Guía de autenticación** con cómo encenderla y qué hacer si algo falla.
- **Guía del túnel de Cloudflare** para mostrar la aplicación por internet de forma temporal.
- **Postman** y la **guía de prueba punto a punto** ya saben iniciar sesión.
- Se eliminó `CLAUDE.md`: sus reglas se movieron al documento principal.
- Se limpiaron las ramas viejas del repositorio: quedan solo las de cada persona, `dev` y `main`.

---

## Cómo lo comprobamos
- Las **83 pruebas automáticas** pasan.
- Se probó con la **base de datos real**, incluyendo la conversión de los roles viejos.
- Se probó **en el navegador** con cada rol: qué pestañas ven, qué botones tienen, y qué pasa con una clave incorrecta o un pase falso.
- Se reconstruyó el sistema completo con el login encendido y funcionó.

---

## Lo que todavía NO está hecho (con honestidad)
| Pendiente | En palabras simples |
|---|---|
| **Avisos automáticos entre partes** | Hoy, cuando un ticket cambia de estado, el aviso a comunicación y estadísticas se envía **a mano**. Falta que sea automático. |
| **Puerta de entrada única (Gateway)** | Falta una sola "puerta" por donde pase todo el tráfico. |
| **Cada solicitante ve solo sus tickets** | El sistema valida el rol, pero todavía no comprueba que un solicitante vea únicamente los suyos. |
| **Tarjeta de satisfacción en el panel** | El promedio de las encuestas aún no aparece en las estadísticas. |
| **Pruebas automáticas al subir cambios** | GitHub todavía no ejecuta las 83 pruebas solo; hoy se corren a mano. |
| **Control de versiones de la base de datos** | Los cambios en la base se aplican al arrancar; falta llevar un historial ordenado. |

---

## Mini guion para exponer (2 minutos)
1. *"Recibimos la parte de comunicación de Gabriel, con comentarios, adjuntos y encuestas de satisfacción."*
2. *"Notamos que el trabajo de un compañero podía mezclarse con el de otro, así que pusimos un vigilante automático: cada quien solo cambia su zona."*
3. *"Agregamos una guía de servicios y un chequeo de salud, como en el proyecto de ejemplo."*
4. *"Y lo más visible: un login con tres roles. El solicitante reporta, el técnico atiende y el administrador supervisa; cada uno ve solo lo que le toca."*
5. *"Todo está probado, documentado y apagado por defecto para no estorbar al equipo. Lo que falta son los avisos automáticos entre servicios y la puerta de entrada única."*

---

## Mini glosario
| Palabra | Significa |
|---|---|
| **Servicio** | Una parte independiente del sistema (tickets, usuarios, comunicación, estadísticas). |
| **Ticket** | Un problema reportado, con su estado: abierto, en progreso o resuelto. |
| **Pase digital (JWT)** | Comprobante que se entrega al iniciar sesión y dice quién eres y qué rol tienes. |
| **Eureka** | La "guía telefónica" donde cada servicio se anota. |
| **Rama** | Una copia del proyecto donde cada persona trabaja sin afectar a las demás. |
| **Pull Request** | Una solicitud para que un cambio sea revisado y aceptado. |
| **Release** | Pasar lo ya probado a la versión estable (`main`). |
