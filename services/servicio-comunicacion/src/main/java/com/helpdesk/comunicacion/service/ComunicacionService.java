package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComunicacionService {

    private final Map<Long, Comentario> comentarios = new ConcurrentHashMap<>();
    private final Map<Long, Notificacion> notificaciones = new ConcurrentHashMap<>();
    private final AtomicLong siguienteComentario = new AtomicLong(1);
    private final AtomicLong siguienteNotificacion = new AtomicLong(1);

    public List<Comentario> obtenerComentarios(Long ticketId) {
        return comentarios.values().stream()
                .filter(c -> ticketId == null || ticketId.equals(c.getTicketId()))
                .sorted(Comparator.comparing(Comentario::getId))
                .toList();
    }

    public Comentario crearComentario(Comentario nuevo) {
        if (nuevo.getTicketId() == null || nuevo.getAutorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y autorId son obligatorios");
        }
        if (nuevo.getTexto() == null || nuevo.getTexto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto es obligatorio");
        }
        nuevo.setId(siguienteComentario.getAndIncrement());
        comentarios.put(nuevo.getId(), nuevo);
        return nuevo;
    }

    public List<Notificacion> obtenerNotificaciones(Long usuarioId, Boolean leida) {
        return notificaciones.values().stream()
                .filter(n -> usuarioId == null || usuarioId.equals(n.getUsuarioId()))
                .filter(n -> leida == null || leida == n.isLeida())
                .sorted(Comparator.comparing(Notificacion::getId))
                .toList();
    }

    public Notificacion marcarLeida(Long id) {
        Notificacion notificacion = notificaciones.get(id);
        if (notificacion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la notificacion con id " + id);
        }
        notificacion.setLeida(true);
        return notificacion;
    }

    public Notificacion alCambiarEstado(TicketEstadoCambiado evento) {
        if (evento.ticketId() == null || evento.solicitanteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y solicitanteId son obligatorios");
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setId(siguienteNotificacion.getAndIncrement());
        notificacion.setUsuarioId(evento.solicitanteId());
        notificacion.setMensaje("Tu ticket #" + evento.ticketId() + " cambio a " + evento.estadoNuevo());
        notificaciones.put(notificacion.getId(), notificacion);
        return notificacion;
    }
}
