package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.model.Ticket;

import java.util.List;

/**
 * Respuesta paginada estable (no expone PageImpl, que cambia entre versiones de Spring).
 */
public record PaginaTickets(
        List<Ticket> contenido,
        int pagina,
        int tamano,
        long total,
        int totalPaginas) {
}
