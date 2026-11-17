package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;

/**
 * Filtros opcionales para listar y exportar tickets. Todos pueden ser null.
 */
public record TicketFiltros(
        EstadoTicket estado,
        Prioridad prioridad,
        Long tecnicoId,
        Long solicitanteId,
        Long categoriaId,
        String q,
        Boolean vencidos) {

    public static TicketFiltros vacios() {
        return new TicketFiltros(null, null, null, null, null, null, null);
    }
}
