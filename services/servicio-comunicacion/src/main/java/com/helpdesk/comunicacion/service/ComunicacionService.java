package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.dto.ContadorNotificacionesResponse;
import com.helpdesk.comunicacion.dto.CrearComentarioRequest;
import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;
import com.helpdesk.comunicacion.model.TipoNotificacion;
import com.helpdesk.comunicacion.repository.ComentarioRepository;
import com.helpdesk.comunicacion.repository.NotificacionRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ComunicacionService {

    private final ComentarioRepository comentarios;
    private final NotificacionRepository notificaciones;
    private final EncuestaService encuestas;
    private final AdjuntoService adjuntos;

    public ComunicacionService(ComentarioRepository comentarios,
                               NotificacionRepository notificaciones,
                               EncuestaService encuestas,
                               AdjuntoService adjuntos) {
        this.comentarios = comentarios;
        this.notificaciones = notificaciones;
        this.encuestas = encuestas;
        this.adjuntos = adjuntos;
    }

    public List<Comentario> obtenerComentarios(Long ticketId) {
        return ticketId == null
                ? comentarios.findAllByOrderByIdAsc()
                : comentarios.findByTicketIdOrderByIdAsc(ticketId);
    }

    public Page<Comentario> obtenerComentariosPaginados(Long ticketId, String q, Pageable pageable) {
        String busqueda = q == null ? "" : q.trim();
        return comentarios.buscar(ticketId, busqueda, pageable);
    }

    @Transactional
    public Comentario crearComentario(CrearComentarioRequest request) {
        if (request == null || request.ticketId() == null || request.autorId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ticketId y autorId son obligatorios");
        }
        if (request.texto() == null || request.texto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto es obligatorio");
        }
        if (request.texto().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El texto no puede superar 1000 caracteres");
        }
        String autorNombre = normalizar(request.autorNombre(), 120, "autorNombre");
        String ticketTitulo = normalizar(request.ticketTitulo(), 200, "ticketTitulo");

        Comentario comentario = new Comentario();
        comentario.setTicketId(request.ticketId());
        comentario.setAutorId(request.autorId());
        comentario.setAutorNombre(autorNombre);
        comentario.setTicketTitulo(ticketTitulo);
        comentario.setTexto(request.texto());
        comentario.setCreadoEn(Instant.now());
        Comentario guardado = comentarios.save(comentario);

        if (request.destinatarioId() != null
                && !request.destinatarioId().equals(request.autorId())) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(request.destinatarioId());
            notificacion.setTicketId(request.ticketId());
            notificacion.setReferenciaId(guardado.getId());
            notificacion.setTipo(TipoNotificacion.COMENTARIO_NUEVO);
            notificacion.setMensaje("Nuevo comentario de "
                    + (autorNombre == null ? "Usuario #" + request.autorId() : autorNombre)
                    + " en el ticket #" + request.ticketId());
            notificacion.setLeida(false);
            notificacion.setCreadaEn(Instant.now());
            notificaciones.save(notificacion);
        }

        return guardado;
    }

    @Transactional
    public Comentario crearComentario(Comentario nuevo) {
        return crearComentario(new CrearComentarioRequest(
                nuevo.getTicketId(),
                nuevo.getAutorId(),
                nuevo.getAutorNombre(),
                nuevo.getTicketTitulo(),
                nuevo.getTexto(),
                null
        ));
    }

    @Transactional
    public void eliminarComentario(Long id) {
        Comentario comentario = comentarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el comentario con id " + id));
        adjuntos.eliminarPorComentario(id);
        comentarios.delete(comentario);
    }

    public List<Notificacion> obtenerNotificaciones(Long usuarioId, Boolean leida,
                                                    TipoNotificacion tipo, Long ticketId) {
        return notificaciones.buscar(usuarioId, leida, tipo, ticketId);
    }

    public ContadorNotificacionesResponse contadorNotificaciones(Long usuarioId) {
        if (usuarioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuarioId es obligatorio");
        }
        return new ContadorNotificacionesResponse(
                notificaciones.countByUsuarioId(usuarioId),
                notificaciones.countByUsuarioIdAndLeidaFalse(usuarioId)
        );
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        if (usuarioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "usuarioId es obligatorio");
        }
        return notificaciones.marcarTodasLeidas(usuarioId, Instant.now());
    }

    @Transactional
    public Notificacion marcarLeida(Long id) {
        Notificacion notificacion = notificaciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la notificacion con id " + id));
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion.setLeidaEn(Instant.now());
            return notificaciones.save(notificacion);
        }
        return notificacion;
    }

    @Transactional
    public Notificacion alCambiarEstado(TicketEstadoCambiado evento) {
        if (evento == null || evento.ticketId() == null || evento.solicitanteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ticketId y solicitanteId son obligatorios");
        }
        if (evento.estadoNuevo() == null || evento.estadoNuevo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "estadoNuevo es obligatorio");
        }

        String estado = evento.estadoNuevo().trim();
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(evento.solicitanteId());
        notificacion.setTicketId(evento.ticketId());
        notificacion.setTipo(TipoNotificacion.TICKET_ESTADO);
        notificacion.setMensaje("Tu ticket #" + evento.ticketId() + " cambio a " + estado);
        notificacion.setLeida(false);
        notificacion.setCreadaEn(Instant.now());
        Notificacion guardada = notificaciones.save(notificacion);

        if ("RESUELTO".equalsIgnoreCase(estado)) {
            encuestas.crearSiNoExiste(evento.ticketId(), evento.solicitanteId())
                    .ifPresent(encuesta -> {
                        Notificacion encuestaNotificacion = new Notificacion();
                        encuestaNotificacion.setUsuarioId(evento.solicitanteId());
                        encuestaNotificacion.setTicketId(evento.ticketId());
                        encuestaNotificacion.setReferenciaId(encuesta.getId());
                        encuestaNotificacion.setTipo(TipoNotificacion.ENCUESTA_SATISFACCION);
                        encuestaNotificacion.setMensaje("Como calificarias la atencion del ticket #"
                                + evento.ticketId() + "? Del 1 al 5");
                        encuestaNotificacion.setLeida(false);
                        encuestaNotificacion.setCreadaEn(Instant.now());
                        notificaciones.save(encuestaNotificacion);
                    });
        }

        return guardada;
    }

    private String normalizar(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    campo + " no puede superar " + maximo + " caracteres");
        }
        return limpio;
    }
}
