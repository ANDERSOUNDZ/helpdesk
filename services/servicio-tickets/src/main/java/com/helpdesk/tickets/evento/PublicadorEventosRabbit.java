package com.helpdesk.tickets.evento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publicador real sobre RabbitMQ (Fase 2). Se activa con EVENTOS_PUBLICADOR=rabbit.
 *
 * Las routing keys y el exchange son el contrato con comunicacion y estadisticas:
 *   ticket.creado, ticket.asignado, ticket.estado-cambiado
 *
 * Si el broker no responde, el ticket se guarda igual (el camino principal no depende del bus)
 * y el fallo queda en el log para revisarlo.
 */
@Component
@ConditionalOnProperty(name = "eventos.publicador", havingValue = "rabbit")
public class PublicadorEventosRabbit implements PublicadorEventos {

    public static final String ROUTING_KEY_CREADO = "ticket.creado";
    public static final String ROUTING_KEY_ASIGNADO = "ticket.asignado";
    public static final String ROUTING_KEY_ESTADO_CAMBIADO = "ticket.estado-cambiado";
    public static final String ROUTING_KEY_SLA_POR_VENCER = "ticket.sla-por-vencer";
    public static final String ROUTING_KEY_SLA_VENCIDO = "ticket.sla-vencido";

    private static final Logger log = LoggerFactory.getLogger(PublicadorEventosRabbit.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public PublicadorEventosRabbit(RabbitTemplate rabbitTemplate,
                                   @Value("${eventos.rabbit.exchange:helpdesk.eventos}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public void publicar(TicketCreadoEvent evento) {
        enviar(ROUTING_KEY_CREADO, evento);
    }

    @Override
    public void publicar(TicketAsignadoEvent evento) {
        enviar(ROUTING_KEY_ASIGNADO, evento);
    }

    @Override
    public void publicar(TicketEstadoCambiadoEvent evento) {
        enviar(ROUTING_KEY_ESTADO_CAMBIADO, evento);
    }

    @Override
    public void publicar(TicketSlaPorVencerEvent evento) {
        enviar(ROUTING_KEY_SLA_POR_VENCER, evento);
    }

    @Override
    public void publicar(TicketSlaVencidoEvent evento) {
        enviar(ROUTING_KEY_SLA_VENCIDO, evento);
    }

    private void enviar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento);
        } catch (AmqpException excepcion) {
            log.error("No se pudo publicar el evento {} en el exchange {}: {}",
                    routingKey, exchange, excepcion.getMessage());
        }
    }
}
