package com.helpdesk.tickets.application.port.in;

import com.helpdesk.tickets.domain.model.Prioridad;
import com.helpdesk.tickets.domain.model.Ticket;

import java.util.List;

// PUERTO DE ENTRADA: lo que el servicio ofrece al exterior. El controller depende de
// esta interfaz y no de la implementacion, asi se puede probar con un doble y cambiar
// la logica sin tocar la capa api.
//
// Es una sola interfaz porque hoy hay 4 operaciones. Si crece, se parte en un caso de
// uso por clase (CrearTicketUseCase, CerrarTicketUseCase...) sin romper a nadie.
public interface GestionarTicketsUseCase {

    Ticket crear(String titulo, String descripcion, Prioridad prioridad, Long solicitanteId);

    List<Ticket> listar();

    Ticket obtenerPorId(Long id);

    Ticket resolver(Long id);
}
