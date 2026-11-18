package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.AccesoTickets;
import com.helpdesk.tickets.dto.ResumenHoras;
import com.helpdesk.tickets.dto.SolicitudHoras;
import com.helpdesk.tickets.model.RegistroHoras;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.RegistroHorasRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HorasServiceTest {

    private final RegistroHorasRepository horas = mock(RegistroHorasRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final HistorialService historial = mock(HistorialService.class);
    private final HorasService service = new HorasService(horas, tickets, historial, new AccesoTickets());

    @BeforeEach
    void preparar() {
        Ticket existente = new Ticket();
        existente.setId(1L);
        when(tickets.findById(1L)).thenReturn(Optional.of(existente));
        when(horas.save(any(RegistroHoras.class))).thenAnswer(llamada -> {
            RegistroHoras registro = llamada.getArgument(0);
            registro.setId(5L);
            return registro;
        });
        when(horas.findByTicketIdOrderByCreadoEnAscIdAsc(1L)).thenReturn(List.of(
                new RegistroHoras(1L, 9L, 90, "diagnostico", Instant.now())));
        when(horas.sumarMinutosPorTicket(1L)).thenReturn(90L);
    }

    @Test
    void registraHorasYDevuelveElTotal() {
        ResumenHoras resumen = service.registrar(1L, new SolicitudHoras(9L, 90, "diagnostico"));

        assertEquals(90, resumen.totalMinutos());
        assertEquals(1.5, resumen.totalHoras());
        assertEquals(1, resumen.registros().size());
        verify(historial).registrar(eq(1L), eq("horas"), isNull(), contains("90 min"));
    }

    @Test
    void rechazaMinutosFueraDeRangoYUnTicketInexistente() {
        assertThrows(ResponseStatusException.class,
                () -> service.registrar(1L, new SolicitudHoras(9L, 0, "x")));
        assertThrows(ResponseStatusException.class,
                () -> service.registrar(1L, new SolicitudHoras(9L, 1441, "x")));
        assertThrows(ResponseStatusException.class,
                () -> service.registrar(99L, new SolicitudHoras(9L, 30, "x")));
    }
}
