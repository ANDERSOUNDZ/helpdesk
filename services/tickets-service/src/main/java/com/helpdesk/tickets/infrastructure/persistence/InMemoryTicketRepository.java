package com.helpdesk.tickets.infrastructure.persistence;

import com.helpdesk.tickets.application.port.out.TicketRepositoryPort;
import com.helpdesk.tickets.domain.model.Ticket;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// ADAPTADOR de salida: implementa el puerto guardando en memoria.
//
// Por que memoria y no H2/PostgreSQL: el objetivo de esta primera version es fijar la
// estructura. Los datos se pierden al reiniciar. Cuando se elija base de datos, se crea
// otro adaptador (TicketJpaAdapter) que implemente el mismo puerto y se borra este;
// ni el dominio ni la aplicacion ni la api cambian.
//
// ConcurrentHashMap y AtomicLong porque Spring atiende varias peticiones a la vez y un
// HashMap normal no es seguro con hilos concurrentes.
@Repository
public class InMemoryTicketRepository implements TicketRepositoryPort {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    @Override
    public Ticket guardar(Ticket ticket) {
        // Si el ticket ya tiene id es una actualizacion; si no, se le asigna uno.
        Long id = ticket.getId() != null ? ticket.getId() : siguienteId.getAndIncrement();
        Ticket guardado = Ticket.reconstruir(id, ticket.getTitulo(), ticket.getDescripcion(),
                ticket.getPrioridad(), ticket.getEstado());
        tickets.put(id, guardado);
        return guardado;
    }

    @Override
    public Optional<Ticket> buscarPorId(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public List<Ticket> buscarTodos() {
        // Copia ordenada por id: el orden de un HashMap no esta garantizado y la API
        // no debe devolver los tickets en desorden de una llamada a otra.
        List<Ticket> lista = new ArrayList<>(tickets.values());
        lista.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return lista;
    }
}
