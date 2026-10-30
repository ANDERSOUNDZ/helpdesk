package com.helpdesk.estadisticas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contadores_prioridad")
public class ContadorPrioridad {

    @Id
    @Column(length = 15)
    private String prioridad;

    @Column(nullable = false)
    private int cantidad;

    public ContadorPrioridad() {
    }

    public ContadorPrioridad(String prioridad, int cantidad) {
        this.prioridad = prioridad;
        this.cantidad = cantidad;
    }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
