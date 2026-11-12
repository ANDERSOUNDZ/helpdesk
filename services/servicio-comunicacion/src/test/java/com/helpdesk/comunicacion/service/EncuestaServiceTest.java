package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.dto.RespuestaEncuestaRequest;
import com.helpdesk.comunicacion.model.EncuestaSatisfaccion;
import com.helpdesk.comunicacion.repository.EncuestaSatisfaccionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncuestaServiceTest {

    private final EncuestaSatisfaccionRepository repo = mock(EncuestaSatisfaccionRepository.class);
    private EncuestaService service;

    @BeforeEach
    void preparar() {
        service = new EncuestaService(repo);
        when(repo.save(any(EncuestaSatisfaccion.class))).thenAnswer(i -> i.getArgument(0));
    }

    private EncuestaSatisfaccion encuesta(Long ticketId, boolean respondida) {
        EncuestaSatisfaccion e = new EncuestaSatisfaccion();
        e.setId(1L);
        e.setTicketId(ticketId);
        e.setSolicitanteId(3L);
        e.setCreadaEn(Instant.now());
        if (respondida) {
            e.setPuntuacion(4);
            e.setRespondidaEn(Instant.now());
        }
        return e;
    }

    @Test
    void creaEncuestaSiNoExiste() {
        when(repo.existsByTicketId(7L)).thenReturn(false);

        Optional<EncuestaSatisfaccion> resultado = service.crearSiNoExiste(7L, 3L);

        assertTrue(resultado.isPresent());
        assertEquals(7L, resultado.get().getTicketId());
        assertFalse(resultado.get().isRespondida());
    }

    @Test
    void noCreaEncuestaDuplicada() {
        when(repo.existsByTicketId(7L)).thenReturn(true);

        assertTrue(service.crearSiNoExiste(7L, 3L).isEmpty());
    }

    @Test
    void respondeEncuestaValida() {
        EncuestaSatisfaccion e = encuesta(7L, false);
        when(repo.findByTicketId(7L)).thenReturn(Optional.of(e));

        EncuestaSatisfaccion respuesta = service.responder(7L, new RespuestaEncuestaRequest(5, "Excelente"));

        assertEquals(5, respuesta.getPuntuacion());
        assertEquals("Excelente", respuesta.getComentario());
        assertTrue(respuesta.isRespondida());
        assertNotNull(respuesta.getRespondidaEn());
    }

    @Test
    void rechazaPuntuacionFueraDeRango() {
        assertThrows(ResponseStatusException.class,
                () -> service.responder(7L, new RespuestaEncuestaRequest(0, null)));
        assertThrows(ResponseStatusException.class,
                () -> service.responder(7L, new RespuestaEncuestaRequest(6, null)));
        assertThrows(ResponseStatusException.class,
                () -> service.responder(7L, new RespuestaEncuestaRequest(null, null)));
    }

    @Test
    void rechazaResponderDosVeces() {
        when(repo.findByTicketId(7L)).thenReturn(Optional.of(encuesta(7L, true)));

        assertThrows(ResponseStatusException.class,
                () -> service.responder(7L, new RespuestaEncuestaRequest(5, "Otra vez")));
    }

    @Test
    void calculaResumenYDistribucion() {
        when(repo.count()).thenReturn(10L);
        when(repo.countByRespondidaEnIsNotNull()).thenReturn(7L);
        when(repo.promedio()).thenReturn(4.2857);
        when(repo.distribucion()).thenReturn(List.of(
                new Object[]{2, 1L},
                new Object[]{4, 2L},
                new Object[]{5, 4L}
        ));

        var resumen = service.resumen();

        assertEquals(10L, resumen.totalEncuestas());
        assertEquals(7L, resumen.respondidas());
        assertEquals(3L, resumen.pendientes());
        assertEquals(4.29, resumen.promedio());
        assertEquals(1L, resumen.distribucion().get(2));
        assertEquals(0L, resumen.distribucion().get(3));
        assertEquals(4L, resumen.distribucion().get(5));
    }

    @Test
    void obtenerEncuestaInexistenteDevuelveError() {
        when(repo.findByTicketId(99L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.obtenerPorTicket(99L));
    }
}
