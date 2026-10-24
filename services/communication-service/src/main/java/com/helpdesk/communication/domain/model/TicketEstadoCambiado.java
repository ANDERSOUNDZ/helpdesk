package com.helpdesk.communication.domain.model;

// Lo que communication-service SABE del evento "un ticket cambio de estado".
// Es su PROPIA copia: no importa nada de tickets-service. Solo declara los campos que le
// sirven (a quien avisar y el nuevo estado); el resto del mensaje se ignora.
//
// El estado va como String y no como enum a proposito: si tickets agrega un estado nuevo,
// este servicio no se rompe ni necesita recompilarse.
public record TicketEstadoCambiado(Long ticketId, String estadoNuevo, Long solicitanteId) {

    // Regla de este servicio: que texto ve el usuario. Es de communication, no de tickets.
    public String mensajeParaSolicitante() {
        return "Tu ticket #" + ticketId + " cambio a " + estadoNuevo;
    }
}
