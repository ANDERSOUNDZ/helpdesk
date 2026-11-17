package com.helpdesk.tickets.dto;

import java.util.List;

public record ResultadoLote(
        int exitosos,
        List<FalloLote> fallidos) {

    public record FalloLote(Long ticketId, String motivo) {
    }
}
