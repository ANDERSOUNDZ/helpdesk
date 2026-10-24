package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private final Map<Long, Ticket> baseDeDatos = new HashMap<>();
    private final TicketRepository repositorio = mock(TicketRepository.class);
    private TicketService service;

    @BeforeEach
    void preparar() {
        when(repositorio.save(any(Ticket.class))).thenAnswer(llamada -> {
            Ticket ticket = llamada.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId((long) baseDeDatos.size() + 1);
            }
            baseDeDatos.put(ticket.getId(), ticket);
            return ticket;
        });
        when(repositorio.findById(anyLong()))
                .thenAnswer(llamada -> Optional.ofNullable(baseDeDatos.get(llamada.<Long>getArgument(0))));
        when(repositorio.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(baseDeDatos.values()));
        service = new TicketService(repositorio);
    }

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
