package com.helpdesk.tickets.domain;

import com.helpdesk.tickets.domain.model.EstadoTicket;
import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test del dominio SIN Spring: arranca en milisegundos. Esa es la ventaja de que el
// dominio sea Java puro.
class TicketTest {

    @Test
    void ticketNuevoNaceAbiertoYSinTecnico() {
        Ticket ticket = Ticket.nuevo("No abre el correo", "Error al iniciar", Prioridad.ALTA, 3L);

        assertEquals(EstadoTicket.ABIERTO, ticket.getEstado());
        assertEquals(3L, ticket.getSolicitanteId());
        assertNull(ticket.getTecnicoId());
    }

    @Test
    void asignarPasaElTicketAEnProgreso() {
        Ticket ticket = Ticket.nuevo("VPN caida", null, Prioridad.MEDIA, 3L);

        ticket.asignarA(9L);

        assertEquals(EstadoTicket.EN_PROGRESO, ticket.getEstado());
        assertEquals(9L, ticket.getTecnicoId());
    }

    @Test
    void resolverCambiaElEstado() {
        Ticket ticket = Ticket.nuevo("VPN caida", null, Prioridad.MEDIA, 3L);

        ticket.resolver();

        assertEquals(EstadoTicket.RESUELTO, ticket.getEstado());
    }

    @Test
    void noSePuedeAsignarUnTicketResuelto() {
        Ticket ticket = Ticket.nuevo("VPN caida", null, Prioridad.MEDIA, 3L);
        ticket.resolver();

        assertThrows(IllegalStateException.class, () -> ticket.asignarA(9L));
    }

    @Test
    void rechazaTituloVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> Ticket.nuevo("   ", "desc", Prioridad.BAJA, 3L));
    }

    @Test
    void rechazaPrioridadNula() {
        assertThrows(IllegalArgumentException.class,
                () -> Ticket.nuevo("Titulo", "desc", null, 3L));
    }

    @Test
    void rechazaSolicitanteNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Ticket.nuevo("Titulo", "desc", Prioridad.BAJA, null));
    }
}
