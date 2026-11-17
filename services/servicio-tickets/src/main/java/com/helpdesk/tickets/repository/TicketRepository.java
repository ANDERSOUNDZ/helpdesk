package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Ticket;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    List<Ticket> findByEstadoNotAndFechaLimiteBeforeAndRecordatorioEnviadoEnIsNull(
            EstadoTicket estado, Instant limite);

    boolean existsByCategoriaId(Long categoriaId);
}
