package com.helpdesk.communication.controller;

import com.helpdesk.communication.model.Comentario;
import com.helpdesk.communication.model.Notificacion;
import com.helpdesk.communication.model.TicketEstadoCambiado;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Un solo controller con los datos en memoria (se pierden al reiniciar): la version mas
// simple posible para probar con Postman. Cuando haya base de datos se separa en capas.
@RestController
public class ComunicacionController {

    private final Map<Long, Comentario> comentarios = new ConcurrentHashMap<>();
    private final Map<Long, Notificacion> notificaciones = new ConcurrentHashMap<>();
    private final AtomicLong siguienteComentario = new AtomicLong(1);
    private final AtomicLong siguienteNotificacion = new AtomicLong(1);

    // ---------- Comentarios ----------

    // ?ticketId=7 filtra los comentarios de un ticket; sin parametro devuelve todos.
    @GetMapping("/comentarios")
    public List<Comentario> listarComentarios(@RequestParam(required = false) Long ticketId) {
        return comentarios.values().stream()
                .filter(c -> ticketId == null || ticketId.equals(c.getTicketId()))
                .sorted(Comparator.comparing(Comentario::getId))
                .toList();
    }

    @PostMapping("/comentarios")
    public ResponseEntity<Comentario> comentar(@RequestBody Comentario nuevo) {
        // No se consulta a tickets ni a identity para comprobar que existen: comentar solo
        // depende de este servicio.
        if (nuevo.getTicketId() == null || nuevo.getAutorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y autorId son obligatorios");
        }
        if (nuevo.getTexto() == null || nuevo.getTexto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto es obligatorio");
        }
        nuevo.setId(siguienteComentario.getAndIncrement());
        comentarios.put(nuevo.getId(), nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // ---------- Notificaciones ----------

    // ?usuarioId=3 devuelve las de un usuario; ?leida=false solo las pendientes.
    @GetMapping("/notificaciones")
    public List<Notificacion> listarNotificaciones(@RequestParam(required = false) Long usuarioId,
                                                   @RequestParam(required = false) Boolean leida) {
        return notificaciones.values().stream()
                .filter(n -> usuarioId == null || usuarioId.equals(n.getUsuarioId()))
                .filter(n -> leida == null || leida == n.isLeida())
                .sorted(Comparator.comparing(Notificacion::getId))
                .toList();
    }

    @PutMapping("/notificaciones/{id}/leida")
    public Notificacion marcarLeida(@PathVariable Long id) {
        Notificacion notificacion = notificaciones.get(id);
        if (notificacion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la notificacion con id " + id);
        }
        notificacion.setLeida(true);
        return notificacion;
    }

    // ---------- Evento de tickets ----------

    // Aqui llegaria el aviso de tickets-service cuando un ticket cambia de estado. Hoy
    // nadie lo llama solo: se simula desde Postman para probar el contrato.
    @PostMapping("/eventos/ticket-estado")
    public ResponseEntity<Notificacion> alCambiarEstado(@RequestBody TicketEstadoCambiado evento) {
        if (evento.solicitanteId() == null || evento.ticketId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y solicitanteId son obligatorios");
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setId(siguienteNotificacion.getAndIncrement());
        notificacion.setUsuarioId(evento.solicitanteId());
        notificacion.setMensaje("Tu ticket #" + evento.ticketId() + " cambio a " + evento.estadoNuevo());
        notificaciones.put(notificacion.getId(), notificacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacion);
    }
}
