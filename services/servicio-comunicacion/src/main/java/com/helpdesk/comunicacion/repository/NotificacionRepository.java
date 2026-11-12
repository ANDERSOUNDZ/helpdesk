package com.helpdesk.comunicacion.repository;

import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TipoNotificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Query("""
            SELECT n FROM Notificacion n
            WHERE (:usuarioId IS NULL OR n.usuarioId = :usuarioId)
              AND (:leida IS NULL OR n.leida = :leida)
              AND (:tipo IS NULL OR n.tipo = :tipo)
              AND (:ticketId IS NULL OR n.ticketId = :ticketId)
            ORDER BY n.id DESC
            """)
    List<Notificacion> buscar(@Param("usuarioId") Long usuarioId,
                              @Param("leida") Boolean leida,
                              @Param("tipo") TipoNotificacion tipo,
                              @Param("ticketId") Long ticketId);

    long countByUsuarioId(Long usuarioId);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying
    @Query("""
            UPDATE Notificacion n
            SET n.leida = true, n.leidaEn = :fecha
            WHERE n.usuarioId = :usuarioId AND n.leida = false
            """)
    int marcarTodasLeidas(@Param("usuarioId") Long usuarioId, @Param("fecha") Instant fecha);
}
