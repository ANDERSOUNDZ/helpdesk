package com.helpdesk.communication.controller;

import com.helpdesk.communication.model.Comentario;
import com.helpdesk.communication.model.Notificacion;
import com.helpdesk.communication.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComunicacionControllerTest {

    private final ComunicacionController controller = new ComunicacionController();

    private Comentario comentario(Long ticketId, Long autorId, String texto) {
        Comentario c = new Comentario();
        c.setTicketId(ticketId);
        c.setAutorId(autorId);
        c.setTexto(texto);
        return c;
    }

    @Test
    void comentarYFiltrarPorTicket() {
        controller.comentar(comentario(7L, 3L, "Ya lo revise"));
        controller.comentar(comentario(8L, 3L, "Otro ticket"));

        assertEquals(1, controller.listarComentarios(7L).size());
        assertEquals(2, controller.listarComentarios(null).size());
    }

    @Test
    void rechazaComentarioSinTextoOSinIds() {
        assertThrows(ResponseStatusException.class, () -> controller.comentar(comentario(7L, 3L, " ")));
        assertThrows(ResponseStatusException.class, () -> controller.comentar(comentario(null, 3L, "hola")));
    }

    @Test
    void elEventoCreaUnaNotificacionSinLeerParaElSolicitante() {
        Notificacion n = controller.alCambiarEstado(new TicketEstadoCambiado(7L, "RESUELTO", 3L)).getBody();

        assertEquals(3L, n.getUsuarioId());
        assertEquals("Tu ticket #7 cambio a RESUELTO", n.getMensaje());
        assertFalse(n.isLeida());
    }

    @Test
    void marcarLeidaLaSacaDePendientes() {
        Notificacion n = controller.alCambiarEstado(new TicketEstadoCambiado(7L, "RESUELTO", 3L)).getBody();

        controller.marcarLeida(n.getId());

        assertTrue(controller.listarNotificaciones(3L, false).isEmpty());
        assertEquals(1, controller.listarNotificaciones(3L, true).size());
    }
}
