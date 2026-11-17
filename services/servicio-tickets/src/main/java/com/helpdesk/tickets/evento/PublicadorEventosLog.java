package com.helpdesk.tickets.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publicador por defecto: deja el evento en el log del servicio.
 * No necesita broker, asi que el servicio se sigue levantando solo (opcion 1 o 2 del README).
 */
@Component
@ConditionalOnProperty(name = "eventos.publicador", havingValue = "log", matchIfMissing = true)
public class PublicadorEventosLog implements PublicadorEventos {

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventosLog.class);

    @Override
    public void publicar(TicketCreadoEvent evento) {
        log.info("evento ticket.creado {}", evento);
    }

    @Override
    public void publicar(TicketAsignadoEvent evento) {
        log.info("evento ticket.asignado {}", evento);
    }

    @Override
    public void publicar(TicketEstadoCambiadoEvent evento) {
        log.info("evento ticket.estado-cambiado {}", evento);
    }

    @Override
    public void publicar(TicketSlaPorVencerEvent evento) {
        log.info("evento ticket.sla-por-vencer {}", evento);
    }

    @Override
    public void publicar(TicketSlaVencidoEvent evento) {
        log.info("evento ticket.sla-vencido {}", evento);
    }
}
