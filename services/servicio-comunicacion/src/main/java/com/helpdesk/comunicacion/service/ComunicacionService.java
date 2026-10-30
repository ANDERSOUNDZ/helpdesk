package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;
import com.helpdesk.comunicacion.repository.ComentarioRepository;
import com.helpdesk.comunicacion.repository.NotificacionRepository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComunicacionService {

    private final ComentarioRepository comentarios;
    private final NotificacionRepository notificaciones;

    public ComunicacionService(ComentarioRepository comentarios, NotificacionRepository notificaciones) {
        this.comentarios = comentarios;
        this.notificaciones = notificaciones;
    }

    public List<Comentario> obtenerComentarios(Long ticketId) {
        return comentarios.findAll(Sort.by("id")).stream()
                .filter(c -> ticketId == null || ticketId.equals(c.getTicketId()))
                .toList();
    }

    public Comentario crearComentario(Comentario nuevo) {
        if (nuevo.getTicketId() == null || nuevo.getAutorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y autorId son obligatorios");
        }
        if (nuevo.getTexto() == null || nuevo.getTexto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto es obligatorio");
        }
        nuevo.setId(null);
        return comentarios.save(nuevo);
    }

    public List<Notificacion> obtenerNotificaciones(Long usuarioId, Boolean leida) {
        return notificaciones.findAll(Sort.by("id")).stream()
                .filter(n -> usuarioId == null || usuarioId.equals(n.getUsuarioId()))
                .filter(n -> leida == null || leida == n.isLeida())
                .toList();
    }

    public Notificacion marcarLeida(Long id) {
        Notificacion notificacion = notificaciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la notificacion con id " + id));
        notificacion.setLeida(true);
        return notificaciones.save(notificacion);
    }

    public void eliminarComentario(Long id) {
        Comentario comentario = comentarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el comentario con id " + id));
        comentarios.delete(comentario);
    }

    public Notificacion alCambiarEstado(TicketEstadoCambiado evento) {
        if (evento.ticketId() == null || evento.solicitanteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y solicitanteId son obligatorios");
        }
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(evento.solicitanteId());
        notificacion.setMensaje("Tu ticket #" + evento.ticketId() + " cambio a " + evento.estadoNuevo());
        return notificaciones.save(notificacion);
    }
}
