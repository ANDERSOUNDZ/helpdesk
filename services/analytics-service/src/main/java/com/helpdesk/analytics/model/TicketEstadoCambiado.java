package com.helpdesk.analytics.model;

// Lo que analytics-service SABE del evento "un ticket cambio de estado". Es su PROPIA
// copia: no importa nada de tickets-service y solo declara los campos que necesita. Los
// estados van como String para que, si tickets agrega uno nuevo, este servicio no se rompa.
// estadoAnterior llega vacio cuando el ticket acaba de crearse.
public record TicketEstadoCambiado(String estadoAnterior, String estadoNuevo) {
}
