package com.helpdesk.tickets.domain.event;

import com.helpdesk.tickets.domain.model.EstadoTicket;

import java.time.Instant;

// CONTRATO con los demas servicios: el hecho "un ticket cambio de estado".
// Es lo unico que tickets cuenta hacia afuera; communication y analytics lo leen cada
// uno con SU PROPIA clase (no comparten esta), asi ninguno depende del codigo de otro.
//
// Por que un evento y no una llamada directa: tickets anuncia lo que paso y no necesita
// saber quien escucha. Si manana se suma un servicio nuevo, tickets no cambia.
//
// Aun no se publica en ningun lado (no hay broker ni llamada HTTP); esta clase fija el
// formato para que todos se pongan de acuerdo desde ya.
public record TicketEstadoCambiado(
        Long ticketId,
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        Long solicitanteId,
        Instant fecha) {
}
