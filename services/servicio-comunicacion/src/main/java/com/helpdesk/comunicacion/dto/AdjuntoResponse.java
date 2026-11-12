package com.helpdesk.comunicacion.dto;

import com.helpdesk.comunicacion.model.Adjunto;

import java.time.Instant;

public record AdjuntoResponse(
        Long id,
        Long comentarioId,
        String nombreOriginal,
        String contentType,
        Long tamanoBytes,
        Instant creadoEn,
        String urlVista,
        String urlDescarga
) {
    public static AdjuntoResponse desde(Adjunto adjunto) {
        return new AdjuntoResponse(
                adjunto.getId(),
                adjunto.getComentarioId(),
                adjunto.getNombreOriginal(),
                adjunto.getContentType(),
                adjunto.getTamanoBytes(),
                adjunto.getCreadoEn(),
                "/adjuntos/" + adjunto.getId(),
                "/adjuntos/" + adjunto.getId() + "/descargar"
        );
    }
}
