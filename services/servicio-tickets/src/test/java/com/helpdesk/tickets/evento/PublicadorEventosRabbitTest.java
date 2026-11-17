package com.helpdesk.tickets.evento;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PublicadorEventosRabbitTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final PublicadorEventosRabbit publicador =
            new PublicadorEventosRabbit(rabbitTemplate, "helpdesk.eventos");

    @Test
    void publicaElTicketCreadoConSuRoutingKey() {
        TicketCreadoEvent evento = new TicketCreadoEvent(1L, "Wifi caido", "ALTA", "ABIERTO", 3L, Instant.now());

        publicador.publicar(evento);

        verify(rabbitTemplate).convertAndSend("helpdesk.eventos", "ticket.creado", evento);
    }

    @Test
    void publicaElTicketAsignadoConSuRoutingKey() {
        TicketAsignadoEvent evento = new TicketAsignadoEvent(
                1L, "Wifi caido", 9L, "ABIERTO", "EN_PROGRESO", 3L, Instant.now());

        publicador.publicar(evento);

        verify(rabbitTemplate).convertAndSend("helpdesk.eventos", "ticket.asignado", evento);
    }

    @Test
    void publicaElCambioDeEstadoConSuRoutingKey() {
        TicketEstadoCambiadoEvent evento = new TicketEstadoCambiadoEvent(
                1L, "Wifi caido", "EN_PROGRESO", "RESUELTO", 3L, 9L, Instant.now());

        publicador.publicar(evento);

        verify(rabbitTemplate).convertAndSend("helpdesk.eventos", "ticket.estado-cambiado", evento);
    }

    @Test
    void publicaElRecordatorioPorVencerConSuRoutingKey() {
        TicketSlaPorVencerEvent evento = new TicketSlaPorVencerEvent(
                1L, "Wifi caido", "ALTA", Instant.now().plusSeconds(1800), 3L, 9L, Instant.now());

        publicador.publicar(evento);

        verify(rabbitTemplate).convertAndSend("helpdesk.eventos", "ticket.sla-por-vencer", evento);
    }

    @Test
    void publicaElAvisoDeVencidoConSuRoutingKey() {
        TicketSlaVencidoEvent evento = new TicketSlaVencidoEvent(
                1L, "Wifi caido", "ALTA", Instant.now().minusSeconds(1800), 3L, 9L, Instant.now());

        publicador.publicar(evento);

        verify(rabbitTemplate).convertAndSend("helpdesk.eventos", "ticket.sla-vencido", evento);
    }
}
