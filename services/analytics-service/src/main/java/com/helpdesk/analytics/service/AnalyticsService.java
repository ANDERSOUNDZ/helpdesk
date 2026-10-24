package com.helpdesk.analytics.service;

import com.helpdesk.analytics.model.ResumenTickets;
import com.helpdesk.analytics.model.TicketEstadoCambiado;

import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private int abiertos;
    private int enProgreso;
    private int resueltos;

    public synchronized ResumenTickets obtenerResumen() {
        return new ResumenTickets(abiertos, enProgreso, resueltos);
    }

    public synchronized ResumenTickets alCambiarEstado(TicketEstadoCambiado evento) {
        sumar(evento.estadoAnterior(), -1);
        sumar(evento.estadoNuevo(), 1);
        return obtenerResumen();
    }

    private void sumar(String estado, int cantidad) {
        if (estado == null) {
            return;
        }
        switch (estado) {
            case "ABIERTO" -> abiertos += cantidad;
            case "EN_PROGRESO" -> enProgreso += cantidad;
            case "RESUELTO" -> resueltos += cantidad;
            default -> { }
        }
    }
}
