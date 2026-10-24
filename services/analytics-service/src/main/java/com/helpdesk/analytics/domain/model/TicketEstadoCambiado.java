package com.helpdesk.analytics.domain.model;

// Lo que analytics-service SABE del evento "un ticket cambio de estado".
// Es su PROPIA copia: no importa nada de tickets-service. Solo declara los campos que
// necesita para actualizar los contadores; el resto del mensaje se ignora.
//
// Los estados van como String y no como enum a proposito: si tickets agrega un estado
// nuevo, este servicio no se rompe ni necesita recompilarse.
public record TicketEstadoCambiado(String estadoAnterior, String estadoNuevo) {
}
