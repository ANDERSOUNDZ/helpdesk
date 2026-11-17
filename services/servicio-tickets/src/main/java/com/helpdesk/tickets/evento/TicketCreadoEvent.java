package com.helpdesk.tickets.evento;

import java.time.Instant;

/**
 * Evento que se publica cuando nace un ticket (Fase 2).
 * Lo consumen comunicacion (aviso al solicitante) y estadisticas (contador de abiertos).
 */
public record TicketCreadoEvent(
        Long ticketId,
        String titulo,
        String prioridad,
        String estado,
        Long solicitanteId,
        Instant fecha) {
}
