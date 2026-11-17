package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.AccesoTickets;
import com.helpdesk.tickets.dto.ResumenHoras;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.dto.SolicitudHoras;
import com.helpdesk.tickets.model.RegistroHoras;
import com.helpdesk.tickets.repository.RegistroHorasRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HorasService {

    private final RegistroHorasRepository horas;
    private final TicketRepository tickets;
    private final HistorialService historial;
    private final AccesoTickets acceso;

    public HorasService(RegistroHorasRepository horas, TicketRepository tickets, HistorialService historial,
                        AccesoTickets acceso) {
        this.horas = horas;
        this.tickets = tickets;
        this.historial = historial;
        this.acceso = acceso;
    }

    public ResumenHoras registrar(Long ticketId, SolicitudHoras solicitud) {
        Ticket ticket = obtenerTicket(ticketId);
        if (solicitud == null || solicitud.tecnicoId() == null || solicitud.tecnicoId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tecnico es obligatorio");
        }
        acceso.exigirRegistrarHoras(ticket, solicitud.tecnicoId());
        if (solicitud.minutos() < 1 || solicitud.minutos() > 1440) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los minutos deben estar entre 1 y 1440");
        }
        String descripcion = solicitud.descripcion() == null ? null : solicitud.descripcion().trim();
        if (descripcion != null && descripcion.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar los 500 caracteres");
        }

        RegistroHoras registro = new RegistroHoras(ticketId, solicitud.tecnicoId(),
                solicitud.minutos(), descripcion, Instant.now());
        RegistroHoras guardado = horas.save(registro);
        historial.registrar(ticketId, "horas", null,
                guardado.getMinutos() + " min" + (descripcion == null ? "" : ": " + descripcion));
        return resumen(ticketId);
    }

    public ResumenHoras obtener(Long ticketId) {
        acceso.exigirVer(obtenerTicket(ticketId));
        return resumen(ticketId);
    }

    public void eliminar(Long ticketId, Long registroId) {
        validarTicket(ticketId);
        RegistroHoras registro = horas.findById(registroId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el registro de horas con id " + registroId));
        if (!ticketId.equals(registro.getTicketId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "El registro de horas no pertenece al ticket " + ticketId);
        }
        horas.delete(registro);
        historial.registrar(ticketId, "horas", registro.getMinutos() + " min", "ELIMINADO");
    }

    private ResumenHoras resumen(Long ticketId) {
        List<RegistroHoras> registros = horas.findByTicketIdOrderByCreadoEnAscIdAsc(ticketId);
        long totalMinutos = horas.sumarMinutosPorTicket(ticketId);
        return new ResumenHoras(ticketId, totalMinutos, totalMinutos / 60.0, registros);
    }

    private void validarTicket(Long ticketId) {
        obtenerTicket(ticketId);
    }

    private Ticket obtenerTicket(Long ticketId) {
        if (ticketId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket con id " + ticketId);
        }
        return tickets.findById(ticketId).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "No existe el ticket con id " + ticketId));
    }
}
