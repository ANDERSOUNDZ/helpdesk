package com.helpdesk.communication.model;

public record TicketEstadoCambiado(Long ticketId, String estadoNuevo, Long solicitanteId) {
}
