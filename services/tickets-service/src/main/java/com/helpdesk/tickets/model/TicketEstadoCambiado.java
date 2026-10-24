package com.helpdesk.tickets.model;

import java.time.Instant;

// CONTRATO con los demas servicios: el hecho "un ticket cambio de estado".
// communication y analytics lo leen cada uno con SU PROPIA clase, asi ninguno depende
// del codigo de otro. Por ahora solo esta definido: nada lo publica todavia.
public record TicketEstadoCambiado(
        Long ticketId,
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        Long solicitanteId,
        Instant fecha) {
}
