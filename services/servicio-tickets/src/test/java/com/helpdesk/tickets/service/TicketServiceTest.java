package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.SlaProperties;
import com.helpdesk.tickets.dto.ResultadoLote;
import com.helpdesk.tickets.dto.SolicitudLoteAsignar;
import com.helpdesk.tickets.dto.SolicitudLoteResolver;
import com.helpdesk.tickets.dto.TicketFiltros;
import com.helpdesk.tickets.evento.PublicadorEventos;
import com.helpdesk.tickets.evento.TicketAsignadoEvent;
import com.helpdesk.tickets.evento.TicketCreadoEvent;
import com.helpdesk.tickets.evento.TicketEstadoCambiadoEvent;
import com.helpdesk.tickets.evento.TicketSlaPorVencerEvent;
import com.helpdesk.tickets.evento.TicketSlaVencidoEvent;
import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.CategoriaRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private final Map<Long, Ticket> baseDeDatos = new HashMap<>();
    private final List<Object> eventosPublicados = new ArrayList<>();
    private final TicketRepository repositorio = mock(TicketRepository.class);
    private final HistorialService historial = mock(HistorialService.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final SlaProperties sla = new SlaProperties();
    private TicketService service;

    @BeforeEach
    void preparar() {
        when(repositorio.save(any(Ticket.class))).thenAnswer(llamada -> {
            Ticket ticket = llamada.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId((long) baseDeDatos.size() + 1);
            }
            baseDeDatos.put(ticket.getId(), ticket);
            return ticket;
        });
        when(repositorio.findById(anyLong()))
                .thenAnswer(llamada -> Optional.ofNullable(baseDeDatos.get(llamada.<Long>getArgument(0))));
        when(repositorio.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(baseDeDatos.values()));
        when(repositorio.findAll(any(Specification.class), any(Sort.class)))
                .thenAnswer(llamada -> new ArrayList<>(baseDeDatos.values()));
        service = new TicketService(repositorio, new RegistroEventos(), historial, categorias, sla);
    }

    private final class RegistroEventos implements PublicadorEventos {
        @Override
        public void publicar(TicketCreadoEvent evento) {
            eventosPublicados.add(evento);
        }

        @Override
        public void publicar(TicketAsignadoEvent evento) {
            eventosPublicados.add(evento);
        }

        @Override
        public void publicar(TicketEstadoCambiadoEvent evento) {
            eventosPublicados.add(evento);
        }

        @Override
        public void publicar(TicketSlaPorVencerEvent evento) {
            eventosPublicados.add(evento);
        }

        @Override
        public void publicar(TicketSlaVencidoEvent evento) {
            eventosPublicados.add(evento);
        }
    }

    private Ticket ticket(String titulo, Long solicitanteId) {
        Ticket t = new Ticket();
        t.setTitulo(titulo);
        t.setPrioridad(Prioridad.ALTA);
        t.setSolicitanteId(solicitanteId);
        return t;
    }

    @Test
    void crearAsignaIdYNaceAbierto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertEquals(1L, creado.getId());
        assertEquals(EstadoTicket.ABIERTO, creado.getEstado());
        assertEquals(null, creado.getTecnicoId());
        assertEquals("Wifi caido", creado.getTitulo());
    }

    @Test
    void crearNormalizaElTituloPublicaElEventoYCalculaElSla() {
        Ticket creado = service.crear(ticket("  Wifi caido  ", 3L));

        assertEquals("Wifi caido", creado.getTitulo());
        assertEquals(1, eventosPublicados.size());
        assertInstanceOf(TicketCreadoEvent.class, eventosPublicados.get(0));
        assertNotNull(creado.getCreadoEn());
        assertNotNull(creado.getFechaLimite());
        assertEquals(8, ChronoUnit.HOURS.between(creado.getCreadoEn(), creado.getFechaLimite()));
        verify(historial).registrar(creado.getId(), "estado", null, "ABIERTO");
    }

    @Test
    void asignarPasaAEnProgresoYResolverAResuelto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertEquals(EstadoTicket.EN_PROGRESO, service.asignar(creado.getId(), 9L).getEstado());
        Ticket resuelto = service.resolver(creado.getId());
        assertEquals(EstadoTicket.RESUELTO, resuelto.getEstado());
        assertNotNull(resuelto.getAsignadoEn());
        assertNotNull(resuelto.getResueltoEn());
    }

    @Test
    void asignarPublicaAsignadoYCambioDeEstado() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        eventosPublicados.clear();

        service.asignar(creado.getId(), 9L);

        assertEquals(2, eventosPublicados.size());
        assertInstanceOf(TicketAsignadoEvent.class, eventosPublicados.get(0));
        assertInstanceOf(TicketEstadoCambiadoEvent.class, eventosPublicados.get(1));
    }

    @Test
    void reasignarPublicaSoloElEventoDeAsignacion() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        eventosPublicados.clear();

        service.asignar(creado.getId(), 10L);

        assertEquals(1, eventosPublicados.size());
        assertInstanceOf(TicketAsignadoEvent.class, eventosPublicados.get(0));
    }

    @Test
    void asignarAlMismoTecnicoEsIdempotenteYNoPublicaEvento() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        eventosPublicados.clear();

        service.asignar(creado.getId(), 9L);

        assertTrue(eventosPublicados.isEmpty());
    }

    @Test
    void resolverPublicaElCambioDeEstado() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        eventosPublicados.clear();

        service.resolver(creado.getId());

        assertEquals(1, eventosPublicados.size());
        TicketEstadoCambiadoEvent evento = assertInstanceOf(TicketEstadoCambiadoEvent.class, eventosPublicados.get(0));
        assertEquals("EN_PROGRESO", evento.estadoAnterior());
        assertEquals("RESUELTO", evento.estadoNuevo());
    }

    @Test
    void reabrirVuelveAEnProgresoYPublicaElCambio() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        service.resolver(creado.getId());
        eventosPublicados.clear();

        Ticket reabierto = service.reabrir(creado.getId());
        assertEquals(EstadoTicket.EN_PROGRESO, reabierto.getEstado());
        assertEquals(null, reabierto.getResueltoEn());
        assertEquals(1, eventosPublicados.size());
    }

    @Test
    void noSePuedeResolverUnTicketAbierto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertThrows(ResponseStatusException.class, () -> service.resolver(creado.getId()));
        assertTrue(eventosPublicados.stream().noneMatch(TicketEstadoCambiadoEvent.class::isInstance));
    }

    @Test
    void noSePuedeResolverUnTicketSinTecnico() {
        Ticket sinTecnico = ticket("Wifi caido", 3L);
        sinTecnico.setId(50L);
        sinTecnico.setEstado(EstadoTicket.EN_PROGRESO);
        sinTecnico.setTecnicoId(null);
        baseDeDatos.put(50L, sinTecnico);

        assertThrows(ResponseStatusException.class, () -> service.resolver(50L));
    }

    @Test
    void noSePuedeResolverDosVeces() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        service.resolver(creado.getId());

        assertThrows(ResponseStatusException.class, () -> service.resolver(creado.getId()));
    }

    @Test
    void noSePuedeAsignarUnTicketResuelto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        service.asignar(creado.getId(), 9L);
        service.resolver(creado.getId());

        assertThrows(ResponseStatusException.class, () -> service.asignar(creado.getId(), 10L));
    }

    @Test
    void soloSePuedeReabrirUnTicketResuelto() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));

        assertThrows(ResponseStatusException.class, () -> service.reabrir(creado.getId()));
    }

    @Test
    void rechazaTituloVacioSolicitanteNuloOTecnicoInvalido() {
        assertThrows(ResponseStatusException.class, () -> service.crear(ticket("  ", 3L)));
        assertThrows(ResponseStatusException.class, () -> service.crear(ticket("Wifi", null)));
        assertThrows(ResponseStatusException.class, () -> service.crear(ticket("Wifi", 0L)));

        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        assertThrows(ResponseStatusException.class, () -> service.asignar(creado.getId(), null));
        assertThrows(ResponseStatusException.class, () -> service.asignar(creado.getId(), 0L));
    }

    @Test
    void rechazaTituloYDescripcionDemasiadoLargos() {
        Ticket tituloLargo = ticket("x".repeat(201), 3L);
        assertThrows(ResponseStatusException.class, () -> service.crear(tituloLargo));

        Ticket descripcionLarga = ticket("Wifi caido", 3L);
        descripcionLarga.setDescripcion("x".repeat(1001));
        assertThrows(ResponseStatusException.class, () -> service.crear(descripcionLarga));
    }

    @Test
    void rechazaCategoriaInexistente() {
        when(categorias.existsById(99L)).thenReturn(false);
        Ticket conCategoria = ticket("Wifi caido", 3L);
        conCategoria.setCategoriaId(99L);

        assertThrows(ResponseStatusException.class, () -> service.crear(conCategoria));
    }

    @Test
    void actualizarPrioridadRecalculaElSla() {
        Ticket creado = service.crear(ticket("Wifi caido", 3L));
        Ticket cambios = new Ticket();
        cambios.setPrioridad(Prioridad.BAJA);

        Ticket actualizado = service.actualizar(creado.getId(), cambios);

        assertEquals(Prioridad.BAJA, actualizado.getPrioridad());
        assertEquals(72, ChronoUnit.HOURS.between(actualizado.getCreadoEn(), actualizado.getFechaLimite()));
    }

    @Test
    void elLoteAsignaTodosYResolverReportaElQueFalla() {
        Ticket primero = service.crear(ticket("Wifi caido", 3L));
        Ticket segundo = service.crear(ticket("Impresora", 4L));

        ResultadoLote asignacion = service.asignarEnLote(
                new SolicitudLoteAsignar(List.of(primero.getId(), segundo.getId()), 9L));
        assertEquals(2, asignacion.exitosos());
        assertTrue(asignacion.fallidos().isEmpty());

        Ticket tercero = service.crear(ticket("VPN", 5L));
        ResultadoLote resolucion = service.resolverEnLote(
                new SolicitudLoteResolver(List.of(primero.getId(), tercero.getId())));
        assertEquals(1, resolucion.exitosos());
        assertEquals(1, resolucion.fallidos().size());
        assertEquals(tercero.getId(), resolucion.fallidos().get(0).ticketId());
    }

    @Test
    void elCsvIncluyeCabeceraYEscapaComas() {
        service.crear(ticket("Wifi, caido", 3L));

        String csv = new String(service.exportarCsv(TicketFiltros.vacios()), StandardCharsets.UTF_8);

        assertTrue(csv.contains("id,titulo,descripcion,prioridad,estado"));
        assertTrue(csv.contains("\"Wifi, caido\""));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> service.obtenerPorId(999L));
    }
}
