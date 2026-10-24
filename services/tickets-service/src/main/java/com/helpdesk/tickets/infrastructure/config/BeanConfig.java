package com.helpdesk.tickets.infrastructure.config;

import com.helpdesk.tickets.application.port.in.GestionarTicketsUseCase;
import com.helpdesk.tickets.application.port.out.TicketRepositoryPort;
import com.helpdesk.tickets.application.service.TicketService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Aqui se "cablea" la aplicacion: es el unico lugar que conoce a la vez el caso de uso
// y el adaptador concreto. Declarar el bean a mano permite que TicketService no lleve
// anotaciones de Spring (ver nota en esa clase).
@Configuration
public class BeanConfig {

    @Bean
    public GestionarTicketsUseCase gestionarTicketsUseCase(TicketRepositoryPort repositorio) {
        return new TicketService(repositorio);
    }
}
