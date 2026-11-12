package com.helpdesk.comunicacion.dto;

public record CrearComentarioRequest(
        Long ticketId,
        Long autorId,
        String autorNombre,
        String ticketTitulo,
        String texto,
        Long destinatarioId
) {
}
