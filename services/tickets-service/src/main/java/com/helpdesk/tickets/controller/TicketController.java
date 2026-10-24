package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Ticket;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Un solo controller con los datos en memoria (se pierden al reiniciar). Es la version
// mas simple posible para probar con Postman; cuando haya base de datos se separa en capas.
//
// ConcurrentHashMap y AtomicLong porque Spring atiende varias peticiones a la vez y un
// HashMap normal no es seguro con hilos concurrentes.
@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    @GetMapping
    public List<Ticket> listar() {
        // Ordenado por id: el orden de un mapa no esta garantizado y la API no debe
        // devolver los tickets en desorden de una llamada a otra.
        return tickets.values().stream().sorted(Comparator.comparing(Ticket::getId)).toList();
    }

    @GetMapping("/{id}")
    public Ticket obtener(@PathVariable Long id) {
        return buscar(id);
    }

    @PostMapping
    public ResponseEntity<Ticket> crear(@RequestBody Ticket nuevo) {
        if (nuevo.getTitulo() == null || nuevo.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio");
        }
        if (nuevo.getPrioridad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La prioridad es obligatoria");
        }
        if (nuevo.getSolicitanteId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El solicitante es obligatorio");
        }
        // Estos tres campos los decide el servidor, no el cliente: aunque Postman los
        // mande en el JSON, se ignoran.
        nuevo.setId(siguienteId.getAndIncrement());
        nuevo.setEstado(EstadoTicket.ABIERTO);
        nuevo.setTecnicoId(null);
        tickets.put(nuevo.getId(), nuevo);
        return ResponseEntity.created(URI.create("/tickets/" + nuevo.getId())).body(nuevo);
    }

    // Tomar el ticket lo pasa a EN_PROGRESO: asignar y empezar son un solo paso para el
    // negocio, asi nunca queda un ticket con tecnico pero aun "ABIERTO".
    @PutMapping("/{id}/asignar/{tecnicoId}")
    public Ticket asignar(@PathVariable Long id, @PathVariable Long tecnicoId) {
        Ticket ticket = buscar(id);
        if (ticket.getEstado() == EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un ticket ya resuelto");
        }
        ticket.setTecnicoId(tecnicoId);
        ticket.setEstado(EstadoTicket.EN_PROGRESO);
        return ticket;
    }

    // PUT y no POST: resolver dos veces deja el mismo resultado (es idempotente).
    @PutMapping("/{id}/resolver")
    public Ticket resolver(@PathVariable Long id) {
        Ticket ticket = buscar(id);
        ticket.setEstado(EstadoTicket.RESUELTO);
        return ticket;
    }

    private Ticket buscar(Long id) {
        Ticket ticket = tickets.get(id);
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket con id " + id);
        }
        return ticket;
    }
}
