package com.helpdesk.communication.model;

// Lo que communication-service SABE del evento "un ticket cambio de estado". Es su PROPIA
// copia: no importa nada de tickets-service y solo declara los campos que le sirven; el
// resto del mensaje se ignora. El estado va como String para que, si tickets agrega un
// estado nuevo, este servicio no se rompa.
public record TicketEstadoCambiado(Long ticketId, String estadoNuevo, Long solicitanteId) {
}
