package com.helpdesk.analytics.controller;

import com.helpdesk.analytics.model.ResumenTickets;
import com.helpdesk.analytics.model.TicketEstadoCambiado;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Un solo controller con los contadores en memoria: la version mas simple posible para
// probar con Postman. No lee la base de datos de tickets-service; se mantiene al dia con
// los avisos de cambio de estado, asi los dos servicios no dependen uno del otro.
@RestController
public class AnalyticsController {

    private int abiertos;
    private int enProgreso;
    private int resueltos;

    @GetMapping("/resumen")
    public synchronized ResumenTickets resumen() {
        return new ResumenTickets(abiertos, enProgreso, resueltos);
    }

    // Aqui llegaria el aviso de tickets-service cuando un ticket cambia de estado. Hoy
    // nadie lo llama solo: se simula desde Postman para probar el contrato.
    // synchronized porque dos avisos a la vez no deben pisarse los contadores.
    @PostMapping("/eventos/ticket-estado")
    public synchronized ResumenTickets alCambiarEstado(@RequestBody TicketEstadoCambiado evento) {
        sumar(evento.estadoAnterior(), -1);
        sumar(evento.estadoNuevo(), 1);
        return resumen();
    }

    // Un estado desconocido se ignora (en vez de fallar), para no romperse si tickets
    // agrega estados nuevos.
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
