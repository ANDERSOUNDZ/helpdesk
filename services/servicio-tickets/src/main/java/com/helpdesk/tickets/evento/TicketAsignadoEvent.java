package com.helpdesk.tickets.evento;

import java.time.Instant;

/**
 * Evento que se publica cuando un ticket se asigna a un tecnico (Fase 2).
 * Lo consume comunicacion para avisar al tecnico de su nueva tarea.
 */
public record TicketAsignadoEvent(
        Long ticketId,
        String titulo,
        Long tecnicoId,
        String estadoAnterior,
        String estadoNuevo,
        Long solicitanteId,
        Instant fecha) {
}
