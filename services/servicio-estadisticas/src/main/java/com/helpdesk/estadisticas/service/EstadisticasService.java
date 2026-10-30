package com.helpdesk.estadisticas.service;

import com.helpdesk.estadisticas.model.ContadorEstado;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;
import com.helpdesk.estadisticas.repository.ContadorEstadoRepository;

import java.util.List;
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

    public List<ContadorEstado> obtenerTodos() {
        return contadores.findAll();
    }

    public ContadorEstado obtenerPorEstado(String estado) {
        String estadoNorm = estado.toUpperCase();
        return contadores.findById(estadoNorm)
                .orElse(new ContadorEstado(estadoNorm, 0));
    }

    public synchronized ContadorEstado ajustarContador(String estado, int cantidad) {
        String estadoNorm = estado.toUpperCase();
        ContadorEstado contador = contadores.findById(estadoNorm).orElse(new ContadorEstado(estadoNorm, 0));
        contador.setCantidad(Math.max(0, cantidad));
        return contadores.save(contador);
    }

    public synchronized void resetear() {
        for (String estado : ESTADOS) {
            contadores.save(new ContadorEstado(estado, 0));
        }
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
