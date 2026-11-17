package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.UsuarioActual;
import com.helpdesk.tickets.model.HistorialTicket;
import com.helpdesk.tickets.repository.HistorialTicketRepository;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class HistorialService {

    private static final int LARGO_MAX_VALOR = 500;
    private static final int LARGO_MAX_CAMPO = 40;

    private final HistorialTicketRepository historial;
    private final UsuarioActual usuarioActual;

    public HistorialService(HistorialTicketRepository historial, UsuarioActual usuarioActual) {
        this.historial = historial;
        this.usuarioActual = usuarioActual;
    }

    public void registrar(Long ticketId, String campo, String valorAnterior, String valorNuevo) {
        historial.save(new HistorialTicket(
                ticketId,
                recortar(campo, LARGO_MAX_CAMPO),
                recortar(valorAnterior, LARGO_MAX_VALOR),
                recortar(valorNuevo, LARGO_MAX_VALOR),
                usuarioActual.id().orElse(null),
                Instant.now()));
    }

    public List<HistorialTicket> obtener(Long ticketId) {
        return historial.findByTicketIdOrderByFechaAscIdAsc(ticketId);
    }

    private String recortar(String valor, int largo) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= largo ? valor : valor.substring(0, largo);
    }
}
