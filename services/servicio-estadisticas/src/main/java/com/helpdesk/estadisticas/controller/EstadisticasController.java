package com.helpdesk.estadisticas.controller;

import com.helpdesk.estadisticas.model.ContadorEstado;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;
import com.helpdesk.estadisticas.service.EstadisticasService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/estadisticas/estados")
    public List<ContadorEstado> listarContadores() {
        return estadisticasService.obtenerTodos();
    }

    @GetMapping("/estadisticas/estados/{estado}")
    public ContadorEstado obtenerContadorPorEstado(@PathVariable String estado) {
        return estadisticasService.obtenerPorEstado(estado);
    }

    @PutMapping("/estadisticas/estados/{estado}")
    public ContadorEstado ajustarContador(@PathVariable String estado, @RequestParam int cantidad) {
        return estadisticasService.ajustarContador(estado, cantidad);
    }

    @DeleteMapping("/estadisticas/reset")
    public ResponseEntity<Void> resetearEstadisticas() {
        estadisticasService.resetear();
        return ResponseEntity.noContent().build();
    }
}
