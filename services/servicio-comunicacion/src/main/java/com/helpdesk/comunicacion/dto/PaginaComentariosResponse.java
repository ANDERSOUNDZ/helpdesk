package com.helpdesk.comunicacion.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaComentariosResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PaginaComentariosResponse<T> desde(Page<T> pagina) {
        return new PaginaComentariosResponse<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast()
        );
    }
}
