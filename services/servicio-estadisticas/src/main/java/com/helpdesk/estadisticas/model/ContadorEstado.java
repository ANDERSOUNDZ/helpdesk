package com.helpdesk.estadisticas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contadores_estado")
public class ContadorEstado {

    @Id
    @Column(length = 15)
    private String estado;

    @Column(nullable = false)
    private int cantidad;

    public ContadorEstado() {
    }

    public ContadorEstado(String estado, int cantidad) {
        this.estado = estado;
        this.cantidad = cantidad;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
