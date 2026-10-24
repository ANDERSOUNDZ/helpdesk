package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepository tickets;

    public TicketService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    public List<Ticket> obtenerTodos() {
        return tickets.findAll(Sort.by("id"));
    }

    public Ticket obtenerPorId(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket con id " + id));
    }

    public Ticket crear(Ticket nuevo) {
        if (nuevo.getTitulo() == null || nuevo.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio");
        }
        if (nuevo.getPrioridad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La prioridad es obligatoria");
        }
        if (nuevo.getSolicitanteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El solicitante es obligatorio");
        }
        nuevo.setId(null);
        nuevo.setEstado(EstadoTicket.ABIERTO);
        nuevo.setTecnicoId(null);
        return tickets.save(nuevo);
    }

    public Ticket asignar(Long id, Long tecnicoId) {
        Ticket ticket = obtenerPorId(id);
        if (ticket.getEstado() == EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un ticket ya resuelto");
        }
        ticket.setTecnicoId(tecnicoId);
        ticket.setEstado(EstadoTicket.EN_PROGRESO);
        return tickets.save(ticket);
    }

    public Ticket resolver(Long id) {
        Ticket ticket = obtenerPorId(id);
        ticket.setEstado(EstadoTicket.RESUELTO);
        return tickets.save(ticket);
    }
}
