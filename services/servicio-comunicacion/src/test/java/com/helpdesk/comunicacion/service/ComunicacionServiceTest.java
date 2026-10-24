package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComunicacionServiceTest {

    private final ComunicacionService service = new ComunicacionService();

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
