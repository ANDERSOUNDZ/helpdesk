package com.helpdesk.analytics.model;

// Lo que muestra el dashboard: cuantos tickets hay en cada estado. NO es una copia de los
// tickets, es un numero calculado: asi, si tickets-service cae, analytics puede seguir
// mostrando el ultimo resumen en vez de caerse con el.
public record ResumenTickets(int abiertos, int enProgreso, int resueltos) {

    public int total() {
        return abiertos + enProgreso + resueltos;
    }
}
