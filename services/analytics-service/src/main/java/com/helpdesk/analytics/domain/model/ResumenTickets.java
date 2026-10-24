package com.helpdesk.analytics.domain.model;

// Resultado que muestra el dashboard: cuantos tickets hay en cada estado.
//
// NO es una copia de los tickets: es un numero calculado. analytics-service no guarda
// tickets ajenos; asi, si tickets-service cae, analytics puede seguir mostrando el
// ultimo resumen que calculo en vez de caerse con el.
//
// Es un record porque es un valor inmutable: dos resumenes con los mismos numeros son iguales.
public record ResumenTickets(int abiertos, int enProgreso, int resueltos) {

    public ResumenTickets {
        // La validacion vive en el dominio: un conteo negativo es un error de calculo.
        if (abiertos < 0 || enProgreso < 0 || resueltos < 0) {
            throw new IllegalArgumentException("Los conteos no pueden ser negativos");
        }
    }

    public int total() {
        return abiertos + enProgreso + resueltos;
    }
}
