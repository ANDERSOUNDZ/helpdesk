package com.helpdesk.comunicacion.model;

public record TicketEstadoCambiado(Long ticketId, String estadoNuevo, Long solicitanteId) {
}
