package com.helpdesk.tickets.api.controller;

import com.helpdesk.tickets.api.dto.CrearTicketRequest;
import com.helpdesk.tickets.api.dto.TicketResponse;
import com.helpdesk.tickets.application.port.in.GestionarTicketsUseCase;
import com.helpdesk.tickets.domain.model.Ticket;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// El controller solo traduce HTTP <-> casos de uso. No tiene ifs de negocio ni de
// "no encontrado": eso lo resuelven el dominio y GlobalExceptionHandler. Asi cada capa
// tiene un solo motivo para cambiar.
@RestController
@RequestMapping("/tickets")
public class TicketController {

    // Depende de la INTERFAZ (puerto de entrada), no de TicketService.
    private final GestionarTicketsUseCase tickets;

    public TicketController(GestionarTicketsUseCase tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    public List<TicketResponse> listar() {
        return tickets.listar().stream().map(TicketResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse obtener(@PathVariable Long id) {
        return TicketResponse.desde(tickets.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@RequestBody CrearTicketRequest request) {
        Ticket creado = tickets.crear(request.titulo(), request.descripcion(),
                request.prioridad(), request.solicitanteId());
        // 201 + cabecera Location con la URL del recurso nuevo: es la convencion REST
        // y permite al cliente saber donde consultarlo sin armar la URL el mismo.
        return ResponseEntity.created(URI.create("/tickets/" + creado.getId()))
                .body(TicketResponse.desde(creado));
    }

    // PUT y no POST: resolver dos veces deja el mismo resultado (es idempotente),
    // y PUT es el verbo que expresa eso.
    @PutMapping("/{id}/resolver")
    public TicketResponse resolver(@PathVariable Long id) {
        return TicketResponse.desde(tickets.resolver(id));
    }
}
