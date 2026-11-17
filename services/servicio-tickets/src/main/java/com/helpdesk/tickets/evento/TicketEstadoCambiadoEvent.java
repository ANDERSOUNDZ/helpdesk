package com.helpdesk.tickets.evento;

import java.time.Instant;

/**
 * Evento oficial de cambio de estado (contrato del README).
 * Lo consumen comunicacion (notificaciones) y estadisticas (contadores).
 */
public record TicketEstadoCambiadoEvent(
        Long ticketId,
        String titulo,
        String estadoAnterior,
        String estadoNuevo,
        Long solicitanteId,
        Long tecnicoId,
        Instant fecha) {
}
