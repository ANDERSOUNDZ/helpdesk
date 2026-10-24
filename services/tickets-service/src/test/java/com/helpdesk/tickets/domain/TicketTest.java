package com.helpdesk.tickets.domain;

import com.helpdesk.tickets.domain.model.EstadoTicket;
import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test del dominio SIN Spring: arranca en milisegundos. Esa es la ventaja de que el
// dominio sea Java puro.
class TicketTest {

    @Test
    void ticketNuevoNaceAbierto() {
        Ticket ticket = Ticket.nuevo("No abre el correo", "Error al iniciar", Prioridad.ALTA);

        assertEquals(EstadoTicket.ABIERTO, ticket.getEstado());
    }

    @Test
    void cerrarCambiaElEstado() {
        Ticket ticket = Ticket.nuevo("No abre el correo", null, Prioridad.MEDIA);

        ticket.cerrar();

        assertEquals(EstadoTicket.CERRADO, ticket.getEstado());
    }

    @Test
    void rechazaTituloVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> Ticket.nuevo("   ", "desc", Prioridad.BAJA));
    }

    @Test
    void rechazaPrioridadNula() {
        assertThrows(IllegalArgumentException.class,
                () -> Ticket.nuevo("Titulo", "desc", null));
    }
}
