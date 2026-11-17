package com.helpdesk.tickets.evento;

import java.time.Instant;

/**
 * Recordatorio de SLA: el ticket esta por vencer y sigue abierto.
 * Lo consume comunicacion para avisar al tecnico y al solicitante.
 */
public record TicketSlaPorVencerEvent(
        Long ticketId,
        String titulo,
        String prioridad,
        Instant fechaLimite,
        Long solicitanteId,
        Long tecnicoId,
        Instant fecha) {
}
