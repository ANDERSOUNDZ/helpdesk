package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.SlaProperties;
import com.helpdesk.tickets.evento.PublicadorEventos;
import com.helpdesk.tickets.evento.TicketSlaPorVencerEvent;
import com.helpdesk.tickets.evento.TicketSlaVencidoEvent;
import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlaServiceTest {

    private final TicketRepository repositorio = mock(TicketRepository.class);
    private final PublicadorEventos eventos = mock(PublicadorEventos.class);
    private final SlaProperties propiedades = new SlaProperties();
    private final SlaService service = new SlaService(repositorio, eventos, propiedades);

    private Ticket ticket(Long id, Instant fechaLimite) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTitulo("Wifi caido");
        t.setPrioridad(Prioridad.ALTA);
        t.setEstado(EstadoTicket.EN_PROGRESO);
        t.setSolicitanteId(3L);
        t.setTecnicoId(9L);
        t.setFechaLimite(fechaLimite);
        return t;
    }

    @Test
    void publicaPorVencerYVencidoYGuardaElRecordatorio() {
        Instant ahora = Instant.parse("2026-01-01T10:00:00Z");
        Ticket porVencer = ticket(1L, ahora.plusSeconds(1800));
        Ticket vencido = ticket(2L, ahora.minusSeconds(1800));
        when(repositorio.findByEstadoNotAndFechaLimiteBeforeAndRecordatorioEnviadoEnIsNull(
                eq(EstadoTicket.RESUELTO), any(Instant.class)))
                .thenReturn(List.of(porVencer, vencido));

        int enviados = service.revisar(ahora);

        assertEquals(2, enviados);
        verify(eventos).publicar(any(TicketSlaPorVencerEvent.class));
        verify(eventos).publicar(any(TicketSlaVencidoEvent.class));
        assertEquals(ahora, porVencer.getRecordatorioEnviadoEn());
        assertEquals(ahora, vencido.getRecordatorioEnviadoEn());
        verify(repositorio).saveAll(anyList());
    }

    @Test
    void siNoHayPendientesNoPublicaNiGuarda() {
        Instant ahora = Instant.parse("2026-01-01T10:00:00Z");
        when(repositorio.findByEstadoNotAndFechaLimiteBeforeAndRecordatorioEnviadoEnIsNull(
                eq(EstadoTicket.RESUELTO), any(Instant.class)))
                .thenReturn(List.of());

        assertEquals(0, service.revisar(ahora));
    }
}
