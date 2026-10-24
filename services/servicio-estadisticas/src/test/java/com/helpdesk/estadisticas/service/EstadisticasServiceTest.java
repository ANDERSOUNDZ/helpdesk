package com.helpdesk.estadisticas.service;

import com.helpdesk.estadisticas.model.ContadorEstado;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.model.TicketEstadoCambiado;
import com.helpdesk.estadisticas.repository.ContadorEstadoRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EstadisticasServiceTest {

    private final Map<String, ContadorEstado> baseDeDatos = new HashMap<>();
    private final ContadorEstadoRepository repositorio = mock(ContadorEstadoRepository.class);
    private EstadisticasService service;

    @BeforeEach
    void preparar() {
        when(repositorio.save(any(ContadorEstado.class))).thenAnswer(llamada -> {
            ContadorEstado contador = llamada.getArgument(0);
            baseDeDatos.put(contador.getEstado(), contador);
            return contador;
        });
        when(repositorio.findById(anyString()))
                .thenAnswer(llamada -> Optional.ofNullable(baseDeDatos.get(llamada.<String>getArgument(0))));
        service = new EstadisticasService(repositorio);
    }

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
