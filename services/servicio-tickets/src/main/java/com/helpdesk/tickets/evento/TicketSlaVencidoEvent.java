package com.helpdesk.tickets.evento;

import java.time.Instant;

/**
 * Aviso de SLA: el ticket supero su fecha limite y sigue abierto.
 */
public record TicketSlaVencidoEvent(
        Long ticketId,
        String titulo,
        String prioridad,
        Instant fechaLimite,
        Long solicitanteId,
        Long tecnicoId,
        Instant fecha) {
}
