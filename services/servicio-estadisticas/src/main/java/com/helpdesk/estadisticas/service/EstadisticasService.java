package com.helpdesk.estadisticas.service;

import com.helpdesk.estadisticas.model.ContadorEstado;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;
import com.helpdesk.estadisticas.repository.ContadorEstadoRepository;

import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class EstadisticasService {

    private static final Set<String> ESTADOS = Set.of("ABIERTO", "EN_PROGRESO", "RESUELTO");

    private final ContadorEstadoRepository contadores;

    public EstadisticasService(ContadorEstadoRepository contadores) {
        this.contadores = contadores;
    }

    public synchronized ResumenTickets obtenerResumen() {
        return new ResumenTickets(cantidad("ABIERTO"), cantidad("EN_PROGRESO"), cantidad("RESUELTO"));
    }

    public synchronized ResumenTickets alCambiarEstado(TicketEstadoCambiado evento) {
        sumar(evento.estadoAnterior(), -1);
        sumar(evento.estadoNuevo(), 1);
        return obtenerResumen();
    }

    private int cantidad(String estado) {
        return contadores.findById(estado).map(ContadorEstado::getCantidad).orElse(0);
    }

    private void sumar(String estado, int cambio) {
        if (estado == null || !ESTADOS.contains(estado)) {
            return;
        }
        ContadorEstado contador = contadores.findById(estado).orElse(new ContadorEstado(estado, 0));
        contador.setCantidad(contador.getCantidad() + cambio);
        contadores.save(contador);
    }
}
