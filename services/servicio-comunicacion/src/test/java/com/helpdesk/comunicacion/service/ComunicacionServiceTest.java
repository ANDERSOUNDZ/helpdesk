package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;
import com.helpdesk.comunicacion.repository.ComentarioRepository;
import com.helpdesk.comunicacion.repository.NotificacionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComunicacionServiceTest {

    private final Map<Long, Comentario> comentariosGuardados = new HashMap<>();
    private final Map<Long, Notificacion> notificacionesGuardadas = new HashMap<>();
    private final ComentarioRepository comentarios = mock(ComentarioRepository.class);
    private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    private ComunicacionService service;

    @BeforeEach
    void preparar() {
        when(comentarios.save(any(Comentario.class))).thenAnswer(llamada -> {
            Comentario comentario = llamada.getArgument(0);
            if (comentario.getId() == null) {
                comentario.setId((long) comentariosGuardados.size() + 1);
            }
            comentariosGuardados.put(comentario.getId(), comentario);
            return comentario;
        });
        when(comentarios.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(comentariosGuardados.values()));

        when(notificaciones.save(any(Notificacion.class))).thenAnswer(llamada -> {
            Notificacion notificacion = llamada.getArgument(0);
            if (notificacion.getId() == null) {
                notificacion.setId((long) notificacionesGuardadas.size() + 1);
            }
            notificacionesGuardadas.put(notificacion.getId(), notificacion);
            return notificacion;
        });
        when(notificaciones.findById(anyLong()))
                .thenAnswer(llamada -> Optional.ofNullable(notificacionesGuardadas.get(llamada.<Long>getArgument(0))));
        when(notificaciones.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(notificacionesGuardadas.values()));

        service = new ComunicacionService(comentarios, notificaciones);
    }

    private Comentario comentario(Long ticketId, Long autorId, String texto) {
        Comentario c = new Comentario();
        c.setTicketId(ticketId);
        c.setAutorId(autorId);
        c.setTexto(texto);
        return c;
    }

    @Test
    void comentarYFiltrarPorTicket() {
        service.crearComentario(comentario(7L, 3L, "Ya lo revise"));
        service.crearComentario(comentario(8L, 3L, "Otro ticket"));

        assertEquals(1, service.obtenerComentarios(7L).size());
        assertEquals(2, service.obtenerComentarios(null).size());
    }

    @Test
    void rechazaComentarioSinTextoOSinIds() {
        assertThrows(ResponseStatusException.class, () -> service.crearComentario(comentario(7L, 3L, " ")));
        assertThrows(ResponseStatusException.class, () -> service.crearComentario(comentario(null, 3L, "hola")));
    }

    @Test
    void elEventoCreaUnaNotificacionSinLeerParaElSolicitante() {
        Notificacion n = service.alCambiarEstado(new TicketEstadoCambiado(7L, "RESUELTO", 3L));

        assertEquals(3L, n.getUsuarioId());
        assertEquals("Tu ticket #7 cambio a RESUELTO", n.getMensaje());
        assertFalse(n.isLeida());
    }

    @Test
    void marcarLeidaLaSacaDePendientes() {
        Notificacion n = service.alCambiarEstado(new TicketEstadoCambiado(7L, "RESUELTO", 3L));

        service.marcarLeida(n.getId());

        assertTrue(service.obtenerNotificaciones(3L, false).isEmpty());
        assertEquals(1, service.obtenerNotificaciones(3L, true).size());
    }
}
