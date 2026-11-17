package com.helpdesk.tickets.evento;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara el exchange de eventos cuando se usa RabbitMQ. Las colas las declaran
 * los servicios consumidores (comunicacion y estadisticas), no tickets.
 */
@Configuration
@ConditionalOnProperty(name = "eventos.publicador", havingValue = "rabbit")
public class EventosRabbitConfig {

    @Bean
    public TopicExchange exchangeDeEventos(
            @Value("${eventos.rabbit.exchange:helpdesk.eventos}") String nombre) {
        return new TopicExchange(nombre, true, false);
    }
}
