package com.helpdesk.estadisticas.controller;

import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;
import com.helpdesk.estadisticas.service.EstadisticasService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping("/resumen")
    public ResumenTickets obtenerResumen() {
        return estadisticasService.obtenerResumen();
    }

    @PostMapping("/eventos/ticket-estado")
    public ResumenTickets recibirCambioDeEstado(@RequestBody TicketEstadoCambiado evento) {
        return estadisticasService.alCambiarEstado(evento);
    }
}
