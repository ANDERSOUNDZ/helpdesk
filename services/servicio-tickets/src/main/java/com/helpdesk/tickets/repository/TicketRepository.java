package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.model.Ticket;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
}
