package com.helpdesk.tickets.api.dto;

import com.helpdesk.tickets.domain.model.EstadoTicket;
import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;

// DTO de SALIDA: lo que el cliente ve. Se devuelve esto y nunca la entidad Ticket,
// para no exponer por accidente campos internos que se agreguen al dominio mas adelante.
public record TicketResponse(Long id, String titulo, String descripcion,
                             Prioridad prioridad, EstadoTicket estado) {

    // La conversion vive aqui y no en el dominio: el dominio no debe conocer los DTO.
    public static TicketResponse desde(Ticket ticket) {
        return new TicketResponse(ticket.getId(), ticket.getTitulo(), ticket.getDescripcion(),
                ticket.getPrioridad(), ticket.getEstado());
    }
}
