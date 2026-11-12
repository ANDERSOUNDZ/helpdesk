package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.Comentario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    List<Comentario> findAllByOrderByIdAsc();

    List<Comentario> findByTicketIdOrderByIdAsc(Long ticketId);

    @Query("""
            SELECT c FROM Comentario c
            WHERE (:ticketId IS NULL OR c.ticketId = :ticketId)
              AND (
                    LOWER(c.texto) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(c.autorNombre, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(c.ticketTitulo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    Page<Comentario> buscar(@Param("ticketId") Long ticketId,
                            @Param("q") String q,
                            Pageable pageable);
}
