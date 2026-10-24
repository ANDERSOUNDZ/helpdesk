package com.helpdesk.estadisticas.service;

import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;

import org.springframework.stereotype.Service;

@Service
public class EstadisticasService {

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
