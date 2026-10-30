package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.service.TicketService;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/tickets")
    public List<Ticket> obtenerTickets() {
        return ticketService.obtenerTodos();
    }

    @GetMapping("/tickets/{id}")
    public Ticket obtenerTicketPorId(@PathVariable Long id) {
        return ticketService.obtenerPorId(id);
    }

    @PostMapping("/tickets")
    public ResponseEntity<Ticket> crearTicket(@RequestBody Ticket nuevoTicket) {
        Ticket creado = ticketService.crear(nuevoTicket);
        return ResponseEntity.created(URI.create("/tickets/" + creado.getId())).body(creado);
    }

    @PutMapping("/tickets/{id}/asignar/{tecnicoId}")
    public Ticket asignarTicket(@PathVariable Long id, @PathVariable Long tecnicoId) {
        return ticketService.asignar(id, tecnicoId);
    }

    @PutMapping("/tickets/{id}/resolver")
    public Ticket resolverTicket(@PathVariable Long id) {
        return ticketService.resolver(id);
    }

    @PutMapping("/tickets/{id}/reabrir")
    public Ticket reabrirTicket(@PathVariable Long id) {
        return ticketService.reabrir(id);
    }

    @PutMapping("/tickets/{id}")
    public Ticket actualizarTicket(@PathVariable Long id, @RequestBody Ticket datos) {
        return ticketService.actualizar(id, datos);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> eliminarTicket(@PathVariable Long id) {
        ticketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
