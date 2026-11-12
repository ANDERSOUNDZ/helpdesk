package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.EncuestaSatisfaccion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EncuestaSatisfaccionRepository extends JpaRepository<EncuestaSatisfaccion, Long> {

    Optional<EncuestaSatisfaccion> findByTicketId(Long ticketId);

    boolean existsByTicketId(Long ticketId);

    long countByRespondidaEnIsNotNull();

    @Query("SELECT AVG(e.puntuacion) FROM EncuestaSatisfaccion e WHERE e.puntuacion IS NOT NULL")
    Double promedio();

    @Query("""
            SELECT e.puntuacion, COUNT(e)
            FROM EncuestaSatisfaccion e
            WHERE e.puntuacion IS NOT NULL
            GROUP BY e.puntuacion
            """)
    List<Object[]> distribucion();
}
