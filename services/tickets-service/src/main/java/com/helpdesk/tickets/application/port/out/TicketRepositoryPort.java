package com.helpdesk.tickets.application.port.out;

import com.helpdesk.tickets.domain.model.Ticket;

import java.util.List;
import java.util.Optional;

// PUERTO DE SALIDA: lo que la aplicacion NECESITA del mundo exterior para guardar tickets.
// La interfaz pertenece a la aplicacion (no a la infraestructura): es la aplicacion la
// que dicta que necesita, y la base de datos se adapta. Por eso hoy se guarda en memoria
// y manana en PostgreSQL sin que TicketService se entere.
public interface TicketRepositoryPort {

    // Devuelve el ticket guardado, ya con su id asignado.
    Ticket guardar(Ticket ticket);

    // Optional en vez de null: obliga a quien llama a decidir que hacer si no existe.
    Optional<Ticket> buscarPorId(Long id);

    List<Ticket> buscarTodos();
}
