package com.helpdesk.analytics.service;

import com.helpdesk.analytics.model.ResumenTickets;
import com.helpdesk.analytics.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsServiceTest {

    private final AnalyticsService service = new AnalyticsService();

    @Test
    void empiezaEnCeros() {
        assertEquals(new ResumenTickets(0, 0, 0), service.obtenerResumen());
    }

    @Test
    void cuentaElCicloCompletoDeUnTicket() {
        service.alCambiarEstado(new TicketEstadoCambiado(null, "ABIERTO"));
        assertEquals(new ResumenTickets(1, 0, 0), service.obtenerResumen());

        service.alCambiarEstado(new TicketEstadoCambiado("ABIERTO", "EN_PROGRESO"));
        assertEquals(new ResumenTickets(0, 1, 0), service.obtenerResumen());

        service.alCambiarEstado(new TicketEstadoCambiado("EN_PROGRESO", "RESUELTO"));
        assertEquals(new ResumenTickets(0, 0, 1), service.obtenerResumen());
        assertEquals(1, service.obtenerResumen().total());
    }

    @Test
    void ignoraUnEstadoDesconocidoEnVezDeFallar() {
        service.alCambiarEstado(new TicketEstadoCambiado(null, "ARCHIVADO"));

        assertEquals(new ResumenTickets(0, 0, 0), service.obtenerResumen());
    }
}
