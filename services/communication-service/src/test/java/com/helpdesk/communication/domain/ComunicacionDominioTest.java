package com.helpdesk.communication.domain;

import com.helpdesk.communication.domain.model.Comentario;
import com.helpdesk.communication.domain.model.Notificacion;
import com.helpdesk.communication.domain.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests del dominio SIN Spring: arrancan en milisegundos.
class ComunicacionDominioTest {

    @Test
    void comentarioGuardaSoloLosIdsYElTexto() {
        Comentario comentario = Comentario.nuevo(7L, 3L, "  Ya lo revise  ");

        assertEquals(7L, comentario.getTicketId());
        assertEquals(3L, comentario.getAutorId());
        assertEquals("Ya lo revise", comentario.getTexto());
    }

    @Test
    void comentarioRechazaTextoVacioOIdsNulos() {
        assertThrows(IllegalArgumentException.class, () -> Comentario.nuevo(7L, 3L, " "));
        assertThrows(IllegalArgumentException.class, () -> Comentario.nuevo(null, 3L, "hola"));
        assertThrows(IllegalArgumentException.class, () -> Comentario.nuevo(7L, null, "hola"));
    }

    @Test
    void notificacionNaceSinLeerYSePuedeMarcarLeida() {
        Notificacion notificacion = Notificacion.nueva(3L, "Tu ticket fue resuelto");
        assertFalse(notificacion.isLeida());

        notificacion.marcarLeida();

        assertTrue(notificacion.isLeida());
    }

    @Test
    void notificacionRechazaMensajeVacio() {
        assertThrows(IllegalArgumentException.class, () -> Notificacion.nueva(3L, ""));
    }

    @Test
    void eventoArmaElMensajeParaElSolicitante() {
        TicketEstadoCambiado evento = new TicketEstadoCambiado(7L, "RESUELTO", 3L);

        assertEquals("Tu ticket #7 cambio a RESUELTO", evento.mensajeParaSolicitante());
    }
}
