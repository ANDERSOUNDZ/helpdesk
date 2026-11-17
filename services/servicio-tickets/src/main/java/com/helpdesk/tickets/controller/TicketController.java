package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.dto.PaginaTickets;
import com.helpdesk.tickets.dto.ResultadoLote;
import com.helpdesk.tickets.dto.ResumenHoras;
import com.helpdesk.tickets.dto.SolicitudHoras;
import com.helpdesk.tickets.dto.SolicitudLoteAsignar;
import com.helpdesk.tickets.dto.SolicitudLoteResolver;
import com.helpdesk.tickets.dto.TicketFiltros;
import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.HistorialTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.service.HistorialService;
import com.helpdesk.tickets.service.HorasService;
import com.helpdesk.tickets.service.TicketService;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class TicketController {

    private final TicketService ticketService;
    private final HistorialService historialService;
    private final HorasService horasService;

    public TicketController(TicketService ticketService, HistorialService historialService,
                            HorasService horasService) {
        this.ticketService = ticketService;
        this.historialService = historialService;
        this.horasService = horasService;
    }

    @GetMapping("/tickets")
    public List<Ticket> obtenerTickets(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean vencidos) {
        return ticketService.buscar(new TicketFiltros(estado, prioridad, tecnicoId,
                solicitanteId, categoriaId, q, vencidos));
    }

    @GetMapping("/tickets/paginado")
    public PaginaTickets obtenerTicketsPaginados(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean vencidos,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ticketService.buscarPaginado(new TicketFiltros(estado, prioridad, tecnicoId,
                solicitanteId, categoriaId, q, vencidos), page, size);
    }

    @GetMapping("/tickets/vencidos")
    public List<Ticket> obtenerVencidos() {
        return ticketService.vencidos();
    }

    @GetMapping(value = "/tickets/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) Prioridad prioridad,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean vencidos) {
        byte[] contenido = ticketService.exportarCsv(new TicketFiltros(estado, prioridad, tecnicoId,
                solicitanteId, categoriaId, q, vencidos));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(contenido);
    }

    @GetMapping("/tickets/{id}")
    public Ticket obtenerTicketPorId(@PathVariable Long id) {
        return ticketService.obtenerParaVer(id);
    }

    @GetMapping("/tickets/{id}/historial")
    public List<HistorialTicket> obtenerHistorial(@PathVariable Long id) {
        ticketService.obtenerParaVer(id);
        return historialService.obtener(id);
    }

    @PostMapping("/tickets")
    public ResponseEntity<Ticket> crearTicket(@Valid @RequestBody Ticket nuevoTicket) {
        Ticket creado = ticketService.crear(nuevoTicket);
        return ResponseEntity.created(URI.create("/tickets/" + creado.getId())).body(creado);
    }

    @PutMapping("/tickets/{id}/asignar/{tecnicoId}")
    public Ticket asignarTicket(@PathVariable Long id, @PathVariable Long tecnicoId) {
        return ticketService.asignar(id, tecnicoId);
    }

    @PutMapping("/tickets/{id}/resolver")
    public Ticket resolverTicket(@PathVariable Long id) {
        return ticketService.resolver(id);
    }

    @PutMapping("/tickets/{id}/reabrir")
    public Ticket reabrirTicket(@PathVariable Long id) {
        return ticketService.reabrir(id);
    }

    @PutMapping("/tickets/{id}")
    public Ticket actualizarTicket(@PathVariable Long id, @Valid @RequestBody Ticket datos) {
        return ticketService.actualizar(id, datos);
    }

    @DeleteMapping("/tickets/{id}")
    public ResponseEntity<Void> eliminarTicket(@PathVariable Long id) {
        ticketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tickets/lote/asignar")
    public ResultadoLote asignarEnLote(@Valid @RequestBody SolicitudLoteAsignar solicitud) {
        return ticketService.asignarEnLote(solicitud);
    }

    @PostMapping("/tickets/lote/resolver")
    public ResultadoLote resolverEnLote(@Valid @RequestBody SolicitudLoteResolver solicitud) {
        return ticketService.resolverEnLote(solicitud);
    }

    @PostMapping("/tickets/{id}/horas")
    public ResponseEntity<ResumenHoras> registrarHoras(@PathVariable Long id,
                                                       @Valid @RequestBody SolicitudHoras solicitud) {
        return ResponseEntity.status(201).body(horasService.registrar(id, solicitud));
    }

    @GetMapping("/tickets/{id}/horas")
    public ResumenHoras obtenerHoras(@PathVariable Long id) {
        return horasService.obtener(id);
    }

    @DeleteMapping("/tickets/{id}/horas/{registroId}")
    public ResponseEntity<Void> eliminarHoras(@PathVariable Long id, @PathVariable Long registroId) {
        horasService.eliminar(id, registroId);
        return ResponseEntity.noContent().build();
    }
}
