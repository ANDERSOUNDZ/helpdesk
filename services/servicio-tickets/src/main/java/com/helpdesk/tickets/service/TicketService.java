package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Ticket;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    public List<Ticket> obtenerTodos() {
        return tickets.values().stream().sorted(Comparator.comparing(Ticket::getId)).toList();
    }

    public Ticket obtenerPorId(Long id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket con id " + id);
        }
        return ticket;
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
        nuevo.setId(siguienteId.getAndIncrement());
        nuevo.setEstado(EstadoTicket.ABIERTO);
        nuevo.setTecnicoId(null);
        tickets.put(nuevo.getId(), nuevo);
        return nuevo;
    }

    public Ticket asignar(Long id, Long tecnicoId) {
        Ticket ticket = obtenerPorId(id);
        if (ticket.getEstado() == EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un ticket ya resuelto");
        }
        ticket.setTecnicoId(tecnicoId);
        ticket.setEstado(EstadoTicket.EN_PROGRESO);
        return ticket;
    }

    public Ticket resolver(Long id) {
        Ticket ticket = obtenerPorId(id);
        ticket.setEstado(EstadoTicket.RESUELTO);
        return ticket;
    }
}
