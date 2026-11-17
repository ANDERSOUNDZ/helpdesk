package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.dto.CrearComentarioRequest;
import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;
import com.helpdesk.comunicacion.model.TipoNotificacion;
import com.helpdesk.comunicacion.repository.ComentarioRepository;
import com.helpdesk.comunicacion.repository.NotificacionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComunicacionServiceTest {

    private final ComentarioRepository comentarios = mock(ComentarioRepository.class);
    private final NotificacionRepository notificaciones = mock(NotificacionRepository.class);
    private final AtomicLong idsComentarios = new AtomicLong(1);
    private final AtomicLong idsNotificaciones = new AtomicLong(1);
    private ComunicacionService service;

    @BeforeEach
    void preparar() {
        when(comentarios.save(any(Comentario.class))).thenAnswer(invocacion -> {
            Comentario comentario = invocacion.getArgument(0);
            if (comentario.getId() == null) {
                comentario.setId(idsComentarios.getAndIncrement());
            }
            return comentario;
        });
        when(notificaciones.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            if (notificacion.getId() == null) {
                notificacion.setId(idsNotificaciones.getAndIncrement());
            }
            return notificacion;
        });
        when(notificaciones.findById(anyLong()))
                .thenReturn(Optional.empty());

        service = new ComunicacionService(comentarios, notificaciones);
    }

    private Comentario comentario(Long ticketId, Long autorId, String texto) {
        Comentario c = new Comentario();
        c.setTicketId(ticketId);
        c.setAutorId(autorId);
        c.setTexto(texto);
        return c;
    }

    @Test
    void comentarYFiltrarPorTicket() {
        Comentario c7 = comentario(7L, 3L, "Ya lo revise");
        Comentario c8 = comentario(8L, 3L, "Otro ticket");
        when(comentarios.findAllByOrderByIdAsc()).thenReturn(List.of(c7, c8));
        when(comentarios.findByTicketIdOrderByIdAsc(7L)).thenReturn(List.of(c7));

        assertEquals(1, service.obtenerComentarios(7L).size());
        assertEquals(2, service.obtenerComentarios(null).size());
    }

    @Test
    void rechazaComentarioSinTextoOSinIds() {
        assertThrows(ResponseStatusException.class, () -> service.crearComentario(
                new CrearComentarioRequest(7L, 3L, null, null, " ", null)));
        assertThrows(ResponseStatusException.class, () -> service.crearComentario(
                new CrearComentarioRequest(null, 3L, null, null, "hola", null)));
    }

    @Test
    void creaComentarioConFechaYNotificaAlDestinatario() {
        Comentario guardado = service.crearComentario(new CrearComentarioRequest(
                7L, 3L, "Ana", "No imprime", "Revisado", 9L));

        assertNotNull(guardado.getCreadoEn());
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).save(captor.capture());
        Notificacion notificacion = captor.getValue();
        assertEquals(9L, notificacion.getUsuarioId());
        assertEquals(7L, notificacion.getTicketId());
        assertEquals(guardado.getId(), notificacion.getReferenciaId());
        assertEquals(TipoNotificacion.COMENTARIO_NUEVO, notificacion.getTipo());
        assertFalse(notificacion.isLeida());
    }

    @Test
    void noNotificaSiElDestinatarioEsElAutor() {
        service.crearComentario(new CrearComentarioRequest(
                7L, 3L, "Ana", "No imprime", "Revisado", 3L));

        verify(notificaciones, never()).save(any(Notificacion.class));
    }

    @Test
    void paginaYBuscaComentariosPorTicket() {
        Comentario c = comentario(7L, 3L, "Falla de red");
        Page<Comentario> pagina = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);
        when(comentarios.buscar(eq(7L), isNull(), eq("red"), any(Pageable.class))).thenReturn(pagina);

        Page<Comentario> resultado = service.obtenerComentariosPaginados(
                7L, null, " red ", PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Falla de red", resultado.getContent().get(0).getTexto());
    }

    @Test
    void paginaYFiltraPorListaDeTickets() {
        Comentario c = comentario(8L, 3L, "Ticket resuelto");
        Page<Comentario> pagina = new PageImpl<>(List.of(c), PageRequest.of(0, 10), 1);
        when(comentarios.buscar(isNull(), eq(List.of(8L, 9L)), eq(""), any(Pageable.class)))
                .thenReturn(pagina);

        Page<Comentario> resultado = service.obtenerComentariosPaginados(
                null, List.of(8L, 9L), "", PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(8L, resultado.getContent().get(0).getTicketId());
    }

    @Test
    void elEventoCreaUnaNotificacionSinLeerParaElSolicitante() {
        Notificacion n = service.alCambiarEstado(new TicketEstadoCambiado(7L, "EN_PROGRESO", 3L));

        assertEquals(3L, n.getUsuarioId());
        assertEquals("Tu ticket #7 cambio a EN_PROGRESO", n.getMensaje());
        assertEquals(TipoNotificacion.TICKET_ESTADO, n.getTipo());
        assertFalse(n.isLeida());
        assertNotNull(n.getCreadaEn());
    }

    @Test
    void elEventoResueltoSoloCreaLaNotificacionDeEstado() {
        Notificacion n = service.alCambiarEstado(new TicketEstadoCambiado(7L, "RESUELTO", 3L));

        assertEquals("Tu ticket #7 cambio a RESUELTO", n.getMensaje());
        assertEquals(TipoNotificacion.TICKET_ESTADO, n.getTipo());
        verify(notificaciones).save(any(Notificacion.class));
    }

    @Test
    void marcarLeidaLaSacaDePendientes() {
        Notificacion n = new Notificacion();
        n.setId(5L);
        n.setUsuarioId(3L);
        n.setMensaje("Aviso");
        n.setLeida(false);
        n.setCreadaEn(Instant.now());
        when(notificaciones.findById(5L)).thenReturn(Optional.of(n));
        when(notificaciones.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));

        Notificacion actualizada = service.marcarLeida(5L);

        assertTrue(actualizada.isLeida());
        assertNotNull(actualizada.getLeidaEn());
    }

    @Test
    void contadorYMarcarTodasDeleganEnElRepositorio() {
        when(notificaciones.countByUsuarioId(3L)).thenReturn(10L);
        when(notificaciones.countByUsuarioIdAndLeidaFalse(3L)).thenReturn(4L);
        when(notificaciones.marcarTodasLeidas(eq(3L), any(Instant.class))).thenReturn(4);

        assertEquals(10L, service.contadorNotificaciones(3L).total());
        assertEquals(4L, service.contadorNotificaciones(3L).noLeidas());
        assertEquals(4, service.marcarTodasLeidas(3L));
    }

    @Test
    void eliminarComentarioBorraSoloElComentario() {
        Comentario c = comentario(7L, 3L, "Texto");
        c.setId(10L);
        when(comentarios.findById(10L)).thenReturn(Optional.of(c));

        service.eliminarComentario(10L);

        verify(comentarios).delete(c);
    }
}
