package com.helpdesk.estadisticas.model;

public record ResumenTickets(int abiertos, int enProgreso, int resueltos) {

    public int total() {
        return abiertos + enProgreso + resueltos;
    }
}
