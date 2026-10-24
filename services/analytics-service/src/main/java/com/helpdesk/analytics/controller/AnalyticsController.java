package com.helpdesk.analytics.controller;

import com.helpdesk.analytics.model.ResumenTickets;
import com.helpdesk.analytics.model.TicketEstadoCambiado;
import com.helpdesk.analytics.service.AnalyticsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/resumen")
    public ResumenTickets obtenerResumen() {
        return analyticsService.obtenerResumen();
    }

    @PostMapping("/eventos/ticket-estado")
    public ResumenTickets recibirCambioDeEstado(@RequestBody TicketEstadoCambiado evento) {
        return analyticsService.alCambiarEstado(evento);
    }
}
