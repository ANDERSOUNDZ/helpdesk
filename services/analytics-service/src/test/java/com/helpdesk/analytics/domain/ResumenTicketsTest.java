package com.helpdesk.analytics.domain;

import com.helpdesk.analytics.domain.model.ResumenTickets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Test del dominio SIN Spring: arranca en milisegundos.
class ResumenTicketsTest {

    @Test
    void totalSumaLosTresEstados() {
        // Ejemplo del enunciado: "esta semana se recibieron 45 y se resolvieron 38".
        ResumenTickets resumen = new ResumenTickets(4, 3, 38);

        assertEquals(45, resumen.total());
    }

    @Test
    void rechazaConteosNegativos() {
        assertThrows(IllegalArgumentException.class, () -> new ResumenTickets(-1, 0, 0));
    }
}
