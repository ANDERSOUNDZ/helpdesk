package com.helpdesk.tickets.application.service;

import com.helpdesk.tickets.application.port.in.GestionarTicketsUseCase;
import com.helpdesk.tickets.application.port.out.TicketRepositoryPort;
import com.helpdesk.tickets.domain.exception.TicketNoEncontradoException;
import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;

import java.util.List;

// Sin @Service a proposito: esta capa no debe importar Spring. El objeto se registra
// como bean en infrastructure/config/BeanConfig. Asi la logica de la aplicacion se
// puede instanciar con "new" en un test, sin contexto de Spring.
//
// Su trabajo es orquestar: pedir cosas al dominio y guardar via el puerto. Las reglas
// de negocio en si (validar titulo, cerrar) estan en Ticket, no aqui.
public class TicketService implements GestionarTicketsUseCase {

    private final TicketRepositoryPort repositorio;

    public TicketService(TicketRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Ticket crear(String titulo, String descripcion, Prioridad prioridad) {
        return repositorio.guardar(Ticket.nuevo(titulo, descripcion, prioridad));
    }

    @Override
    public List<Ticket> listar() {
        return repositorio.buscarTodos();
    }

    @Override
    public Ticket obtenerPorId(Long id) {
        // Lanzar la excepcion aqui (y no devolver null) mantiene al controller sin ifs.
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new TicketNoEncontradoException(id));
    }

    @Override
    public Ticket cerrar(Long id) {
        Ticket ticket = obtenerPorId(id);
        ticket.cerrar();
        return repositorio.guardar(ticket);
    }
}
