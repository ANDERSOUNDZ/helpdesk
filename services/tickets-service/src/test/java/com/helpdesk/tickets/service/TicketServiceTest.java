package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketServiceTest {

    private final TicketService service = new TicketService();

    private Ticket ticket(String titulo, Long solicitanteId) {
        Ticket t = new Ticket();
        t.setTitulo(titulo);
        t.setPrioridad(Prioridad.ALTA);
        t.setSolicitanteId(solicitanteId);
        return t;
    }

    @Test
    void crearAsignaIdYNaceAbierto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertEquals(1L, creado.getId());
        assertEquals(EstadoTicket.ABIERTO, creado.getEstado());
    }

    @Test
    void asignarPasaAEnProgresoYResolverAResuelto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertEquals(EstadoTicket.EN_PROGRESO, service.asignar(creado.getId(), 9L).getEstado());
        assertEquals(EstadoTicket.RESUELTO, service.resolver(creado.getId()).getEstado());
    }

    @Test
    void noSePuedeAsignarUnTicketResuelto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.resolver(creado.getId());

        assertThrows(ResponseStatusException.class, () -> service.asignar(creado.getId(), 9L));
    }

    @Test
    void rechazaTituloVacioYSolicitanteNulo() {
        assertThrows(ResponseStatusException.class, () -> service.crear(ticket("  ", 3L)));
        assertThrows(ResponseStatusException.class, () -> service.crear(ticket("Wifi", null)));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> service.obtenerPorId(999L));
    }
}
