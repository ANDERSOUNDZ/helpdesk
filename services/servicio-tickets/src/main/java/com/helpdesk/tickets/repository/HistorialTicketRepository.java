package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.model.HistorialTicket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialTicketRepository extends JpaRepository<HistorialTicket, Long> {

    List<HistorialTicket> findByTicketIdOrderByFechaAscIdAsc(Long ticketId);
}
