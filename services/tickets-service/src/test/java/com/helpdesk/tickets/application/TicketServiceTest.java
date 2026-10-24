package com.helpdesk.tickets.application;

import com.helpdesk.tickets.application.service.TicketService;
import com.helpdesk.tickets.domain.exception.TicketNoEncontradoException;
import com.helpdesk.tickets.domain.model.EstadoTicket;
import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;
import com.helpdesk.tickets.infrastructure.persistence.InMemoryTicketRepository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// TicketService se crea con "new": no hace falta Spring porque la capa application
// no depende de el. Se usa el repositorio en memoria como adaptador real y simple.
class TicketServiceTest {

    private final TicketService service = new TicketService(new InMemoryTicketRepository());

    @Test
    void crearAsignaIdYListaElTicket() {
        Ticket creado = service.crear("Impresora no imprime", "Sala 2", Prioridad.MEDIA);

        assertNotNull(creado.getId());
        assertEquals(1, service.listar().size());
    }

    @Test
    void cerrarPersisteElNuevoEstado() {
        Ticket creado = service.crear("VPN caida", null, Prioridad.ALTA);

        service.cerrar(creado.getId());

        assertEquals(EstadoTicket.CERRADO, service.obtenerPorId(creado.getId()).getEstado());
    }

    @Test
    void obtenerUnIdInexistenteLanzaExcepcionDeDominio() {
        assertThrows(TicketNoEncontradoException.class, () -> service.obtenerPorId(999L));
    }
}
