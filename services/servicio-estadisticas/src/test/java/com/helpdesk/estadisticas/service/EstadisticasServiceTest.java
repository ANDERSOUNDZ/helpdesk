package com.helpdesk.estadisticas.service;

import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EstadisticasServiceTest {

    private final EstadisticasService service = new EstadisticasService();

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
