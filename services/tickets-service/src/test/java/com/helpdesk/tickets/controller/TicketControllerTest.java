package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Se prueba el controller como una clase normal, sin levantar Spring: es rapido.
class TicketControllerTest {

    private final TicketController controller = new TicketController();

    private Ticket ticket(String titulo, Long solicitanteId) {
        Ticket t = new Ticket();
        t.setTitulo(titulo);
        t.setPrioridad(Prioridad.ALTA);
        t.setSolicitanteId(solicitanteId);
        return t;
    }

    @Test
    void crearAsignaIdYNaceAbierto() {
        Ticket creado = controller.crear(ticket("Wifi caido", 3L)).getBody();

        assertEquals(1L, creado.getId());
        assertEquals(EstadoTicket.ABIERTO, creado.getEstado());
    }

    @Test
    void asignarPasaAEnProgresoYResolverAResuelto() {
        Ticket creado = controller.crear(ticket("Wifi caido", 3L)).getBody();

        assertEquals(EstadoTicket.EN_PROGRESO, controller.asignar(creado.getId(), 9L).getEstado());
        assertEquals(EstadoTicket.RESUELTO, controller.resolver(creado.getId()).getEstado());
    }

    @Test
    void noSePuedeAsignarUnTicketResuelto() {
        Ticket creado = controller.crear(ticket("Wifi caido", 3L)).getBody();
        controller.resolver(creado.getId());

        assertThrows(ResponseStatusException.class, () -> controller.asignar(creado.getId(), 9L));
    }

    @Test
    void rechazaTituloVacioYSolicitanteNulo() {
        assertThrows(ResponseStatusException.class, () -> controller.crear(ticket("  ", 3L)));
        assertThrows(ResponseStatusException.class, () -> controller.crear(ticket("Wifi", null)));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> controller.obtener(999L));
    }
}
