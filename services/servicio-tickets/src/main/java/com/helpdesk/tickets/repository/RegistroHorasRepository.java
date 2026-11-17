package com.helpdesk.tickets.repository;

import com.helpdesk.tickets.model.RegistroHoras;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroHorasRepository extends JpaRepository<RegistroHoras, Long> {

    List<RegistroHoras> findByTicketIdOrderByCreadoEnAscIdAsc(Long ticketId);

    @Query("select coalesce(sum(r.minutos), 0) from RegistroHoras r where r.ticketId = :ticketId")
    long sumarMinutosPorTicket(@Param("ticketId") Long ticketId);
}
