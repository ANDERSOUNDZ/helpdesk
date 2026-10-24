package com.helpdesk.analytics.controller;

import com.helpdesk.analytics.model.ResumenTickets;
import com.helpdesk.analytics.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsControllerTest {

    private final AnalyticsController controller = new AnalyticsController();

    @Test
    void empiezaEnCeros() {
        assertEquals(new ResumenTickets(0, 0, 0), controller.resumen());
    }

    @Test
    void cuentaElCicloCompletoDeUnTicket() {
        controller.alCambiarEstado(new TicketEstadoCambiado(null, "ABIERTO"));
        assertEquals(new ResumenTickets(1, 0, 0), controller.resumen());

        controller.alCambiarEstado(new TicketEstadoCambiado("ABIERTO", "EN_PROGRESO"));
        assertEquals(new ResumenTickets(0, 1, 0), controller.resumen());

        controller.alCambiarEstado(new TicketEstadoCambiado("EN_PROGRESO", "RESUELTO"));
        assertEquals(new ResumenTickets(0, 0, 1), controller.resumen());
        assertEquals(1, controller.resumen().total());
    }

    @Test
    void ignoraUnEstadoDesconocidoEnVezDeFallar() {
        controller.alCambiarEstado(new TicketEstadoCambiado(null, "ARCHIVADO"));

        assertEquals(new ResumenTickets(0, 0, 0), controller.resumen());
    }
}
