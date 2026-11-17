package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.SlaProperties;
import com.helpdesk.tickets.dto.PaginaTickets;
import com.helpdesk.tickets.dto.ResultadoLote;
import com.helpdesk.tickets.dto.ResultadoLote.FalloLote;
import com.helpdesk.tickets.dto.SolicitudLoteAsignar;
import com.helpdesk.tickets.dto.SolicitudLoteResolver;
import com.helpdesk.tickets.dto.TicketFiltros;
import com.helpdesk.tickets.evento.PublicadorEventos;
import com.helpdesk.tickets.evento.TicketAsignadoEvent;
import com.helpdesk.tickets.evento.TicketCreadoEvent;
import com.helpdesk.tickets.evento.TicketEstadoCambiadoEvent;
import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.CategoriaRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;

@Service
public class TicketService {

    private static final int LARGO_MAX_TITULO = 200;
    private static final int LARGO_MAX_DESCRIPCION = 1000;
    private static final int MAX_TICKETS_POR_LOTE = 100;

    private final TicketRepository tickets;
    private final PublicadorEventos eventos;
    private final HistorialService historial;
    private final CategoriaRepository categorias;
    private final SlaProperties sla;

    public TicketService(TicketRepository tickets, PublicadorEventos eventos, HistorialService historial,
                         CategoriaRepository categorias, SlaProperties sla) {
        this.tickets = tickets;
        this.eventos = eventos;
        this.historial = historial;
        this.categorias = categorias;
        this.sla = sla;
    }

    public List<Ticket> obtenerTodos() {
        return buscar(TicketFiltros.vacios());
    }

    public List<Ticket> buscar(TicketFiltros filtros) {
        return tickets.findAll(especificacion(filtros), Sort.by("id"));
    }

    public PaginaTickets buscarPaginado(TicketFiltros filtros, int pagina, int tamano) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), 100);
        Page<Ticket> resultado = tickets.findAll(especificacion(filtros),
                PageRequest.of(paginaSegura, tamanoSeguro, Sort.by("id")));
        return new PaginaTickets(resultado.getContent(), resultado.getNumber(), resultado.getSize(),
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    public List<Ticket> vencidos() {
        return buscar(new TicketFiltros(null, null, null, null, null, null, true));
    }

    public Ticket obtenerPorId(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket con id " + id));
    }

    public Ticket crear(Ticket nuevo) {
        nuevo.setTitulo(validarTitulo(nuevo.getTitulo()));
        validarDescripcion(nuevo.getDescripcion());
        if (nuevo.getPrioridad() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La prioridad es obligatoria");
        }
        validarIdPositivo(nuevo.getSolicitanteId(), "El solicitante es obligatorio");
        if (nuevo.getCategoriaId() != null) {
            validarCategoria(nuevo.getCategoriaId());
        }

        Instant ahora = Instant.now();
        nuevo.setId(null);
        nuevo.setEstado(EstadoTicket.ABIERTO);
        nuevo.setTecnicoId(null);
        nuevo.setCreadoEn(ahora);
        nuevo.setAsignadoEn(null);
        nuevo.setResueltoEn(null);
        nuevo.setRecordatorioEnviadoEn(null);
        nuevo.setFechaLimite(calcularFechaLimite(ahora, nuevo.getPrioridad()));
        Ticket guardado = tickets.save(nuevo);

        historial.registrar(guardado.getId(), "estado", null, EstadoTicket.ABIERTO.name());
        eventos.publicar(new TicketCreadoEvent(
                guardado.getId(),
                guardado.getTitulo(),
                guardado.getPrioridad().name(),
                guardado.getEstado().name(),
                guardado.getSolicitanteId(),
                ahora));
        return guardado;
    }

    public Ticket asignar(Long id, Long tecnicoId) {
        validarIdPositivo(tecnicoId, "El tecnico es obligatorio");
        Ticket ticket = obtenerPorId(id);
        if (ticket.getEstado() == EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede asignar un ticket ya resuelto");
        }
        if (ticket.getEstado() == EstadoTicket.EN_PROGRESO && tecnicoId.equals(ticket.getTecnicoId())) {
            return ticket;
        }

        EstadoTicket anterior = ticket.getEstado();
        Long tecnicoAnterior = ticket.getTecnicoId();
        Instant ahora = Instant.now();
        ticket.setTecnicoId(tecnicoId);
        ticket.setAsignadoEn(ahora);
        ticket.setEstado(EstadoTicket.EN_PROGRESO);
        Ticket guardado = tickets.save(ticket);

        historial.registrar(guardado.getId(), "tecnicoId",
                tecnicoAnterior == null ? null : tecnicoAnterior.toString(), tecnicoId.toString());
        if (anterior != EstadoTicket.EN_PROGRESO) {
            historial.registrar(guardado.getId(), "estado", anterior.name(), EstadoTicket.EN_PROGRESO.name());
        }
        eventos.publicar(new TicketAsignadoEvent(
                guardado.getId(),
                guardado.getTitulo(),
                guardado.getTecnicoId(),
                anterior.name(),
                guardado.getEstado().name(),
                guardado.getSolicitanteId(),
                ahora));
        if (anterior != EstadoTicket.EN_PROGRESO) {
            eventos.publicar(new TicketEstadoCambiadoEvent(
                    guardado.getId(),
                    guardado.getTitulo(),
                    anterior.name(),
                    guardado.getEstado().name(),
                    guardado.getSolicitanteId(),
                    guardado.getTecnicoId(),
                    ahora));
        }
        return guardado;
    }

    public Ticket resolver(Long id) {
        Ticket ticket = obtenerPorId(id);
        if (ticket.getEstado() == EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El ticket ya esta resuelto");
        }
        if (ticket.getEstado() != EstadoTicket.EN_PROGRESO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede resolver un ticket en estado EN_PROGRESO");
        }
        if (ticket.getTecnicoId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El ticket no tiene un tecnico asignado");
        }

        EstadoTicket anterior = ticket.getEstado();
        Instant ahora = Instant.now();
        ticket.setEstado(EstadoTicket.RESUELTO);
        ticket.setResueltoEn(ahora);
        Ticket guardado = tickets.save(ticket);

        historial.registrar(guardado.getId(), "estado", anterior.name(), EstadoTicket.RESUELTO.name());
        eventos.publicar(new TicketEstadoCambiadoEvent(
                guardado.getId(),
                guardado.getTitulo(),
                anterior.name(),
                guardado.getEstado().name(),
                guardado.getSolicitanteId(),
                guardado.getTecnicoId(),
                ahora));
        return guardado;
    }

    public Ticket reabrir(Long id) {
        Ticket ticket = obtenerPorId(id);
        if (ticket.getEstado() != EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede reabrir un ticket resuelto");
        }

        EstadoTicket anterior = ticket.getEstado();
        ticket.setEstado(EstadoTicket.EN_PROGRESO);
        ticket.setResueltoEn(null);
        ticket.setRecordatorioEnviadoEn(null);
        Ticket guardado = tickets.save(ticket);

        historial.registrar(guardado.getId(), "estado", anterior.name(), EstadoTicket.EN_PROGRESO.name());
        eventos.publicar(new TicketEstadoCambiadoEvent(
                guardado.getId(),
                guardado.getTitulo(),
                anterior.name(),
                guardado.getEstado().name(),
                guardado.getSolicitanteId(),
                guardado.getTecnicoId(),
                Instant.now()));
        return guardado;
    }

    public Ticket actualizar(Long id, Ticket datos) {
        Ticket ticket = obtenerPorId(id);

        if (datos.getTitulo() != null) {
            String nuevoTitulo = validarTitulo(datos.getTitulo());
            if (!nuevoTitulo.equals(ticket.getTitulo())) {
                historial.registrar(id, "titulo", ticket.getTitulo(), nuevoTitulo);
                ticket.setTitulo(nuevoTitulo);
            }
        }
        if (datos.getDescripcion() != null) {
            validarDescripcion(datos.getDescripcion());
            if (!Objects.equals(datos.getDescripcion(), ticket.getDescripcion())) {
                historial.registrar(id, "descripcion", ticket.getDescripcion(), datos.getDescripcion());
                ticket.setDescripcion(datos.getDescripcion());
            }
        }
        if (datos.getPrioridad() != null && datos.getPrioridad() != ticket.getPrioridad()) {
            Prioridad anterior = ticket.getPrioridad();
            ticket.setPrioridad(datos.getPrioridad());
            Instant base = ticket.getCreadoEn() != null ? ticket.getCreadoEn() : Instant.now();
            ticket.setFechaLimite(calcularFechaLimite(base, datos.getPrioridad()));
            ticket.setRecordatorioEnviadoEn(null);
            historial.registrar(id, "prioridad", anterior == null ? null : anterior.name(),
                    datos.getPrioridad().name());
        }
        if (datos.getCategoriaId() != null && !datos.getCategoriaId().equals(ticket.getCategoriaId())) {
            validarCategoria(datos.getCategoriaId());
            historial.registrar(id, "categoriaId",
                    ticket.getCategoriaId() == null ? null : ticket.getCategoriaId().toString(),
                    datos.getCategoriaId().toString());
            ticket.setCategoriaId(datos.getCategoriaId());
        }
        return tickets.save(ticket);
    }

    public void eliminar(Long id) {
        Ticket ticket = obtenerPorId(id);
        historial.registrar(id, "ticket", null, "ELIMINADO");
        tickets.delete(ticket);
    }

    public ResultadoLote asignarEnLote(SolicitudLoteAsignar solicitud) {
        List<Long> ids = limpiarIds(solicitud == null ? null : solicitud.ticketIds());
        Long tecnicoId = solicitud == null ? null : solicitud.tecnicoId();
        validarIdPositivo(tecnicoId, "El tecnico es obligatorio");

        int exitosos = 0;
        List<FalloLote> fallidos = new ArrayList<>();
        for (Long ticketId : ids) {
            try {
                asignar(ticketId, tecnicoId);
                exitosos++;
            } catch (ResponseStatusException excepcion) {
                fallidos.add(new FalloLote(ticketId, motivo(excepcion)));
            }
        }
        return new ResultadoLote(exitosos, fallidos);
    }

    public ResultadoLote resolverEnLote(SolicitudLoteResolver solicitud) {
        List<Long> ids = limpiarIds(solicitud == null ? null : solicitud.ticketIds());

        int exitosos = 0;
        List<FalloLote> fallidos = new ArrayList<>();
        for (Long ticketId : ids) {
            try {
                resolver(ticketId);
                exitosos++;
            } catch (ResponseStatusException excepcion) {
                fallidos.add(new FalloLote(ticketId, motivo(excepcion)));
            }
        }
        return new ResultadoLote(exitosos, fallidos);
    }

    public byte[] exportarCsv(TicketFiltros filtros) {
        List<Ticket> lista = buscar(filtros);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("id,titulo,descripcion,prioridad,estado,categoria_id,solicitante_id,tecnico_id,")
                .append("creado_en,asignado_en,resuelto_en,fecha_limite,vencido\n");
        for (Ticket ticket : lista) {
            csv.append(valor(ticket.getId())).append(',')
                    .append(csv(ticket.getTitulo())).append(',')
                    .append(csv(ticket.getDescripcion())).append(',')
                    .append(valor(ticket.getPrioridad())).append(',')
                    .append(valor(ticket.getEstado())).append(',')
                    .append(valor(ticket.getCategoriaId())).append(',')
                    .append(valor(ticket.getSolicitanteId())).append(',')
                    .append(valor(ticket.getTecnicoId())).append(',')
                    .append(valor(ticket.getCreadoEn())).append(',')
                    .append(valor(ticket.getAsignadoEn())).append(',')
                    .append(valor(ticket.getResueltoEn())).append(',')
                    .append(valor(ticket.getFechaLimite())).append(',')
                    .append(ticket.isVencido() ? "SI" : "NO").append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Specification<Ticket> especificacion(TicketFiltros filtros) {
        return (root, query, cb) -> {
            if (filtros == null) {
                return cb.conjunction();
            }
            List<Predicate> condiciones = new ArrayList<>();
            if (filtros.estado() != null) {
                condiciones.add(cb.equal(root.get("estado"), filtros.estado()));
            }
            if (filtros.prioridad() != null) {
                condiciones.add(cb.equal(root.get("prioridad"), filtros.prioridad()));
            }
            if (filtros.tecnicoId() != null) {
                condiciones.add(cb.equal(root.get("tecnicoId"), filtros.tecnicoId()));
            }
            if (filtros.solicitanteId() != null) {
                condiciones.add(cb.equal(root.get("solicitanteId"), filtros.solicitanteId()));
            }
            if (filtros.categoriaId() != null) {
                condiciones.add(cb.equal(root.get("categoriaId"), filtros.categoriaId()));
            }
            if (filtros.q() != null && !filtros.q().isBlank()) {
                String patron = "%" + filtros.q().trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.<String>get("titulo")), patron),
                        cb.like(cb.lower(root.<String>get("descripcion")), patron)));
            }
            if (Boolean.TRUE.equals(filtros.vencidos())) {
                condiciones.add(cb.lessThan(root.get("fechaLimite"), Instant.now()));
                condiciones.add(cb.notEqual(root.get("estado"), EstadoTicket.RESUELTO));
            }
            return condiciones.isEmpty() ? cb.conjunction() : cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    private Instant calcularFechaLimite(Instant base, Prioridad prioridad) {
        return base.plus(sla.horasPara(prioridad), ChronoUnit.HOURS);
    }

    private List<Long> limpiarIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de tickets es obligatoria");
        }
        List<Long> limpios = new ArrayList<>(new LinkedHashSet<>(ids));
        limpios.removeIf(Objects::isNull);
        if (limpios.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La lista de tickets es obligatoria");
        }
        if (limpios.size() > MAX_TICKETS_POR_LOTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pueden procesar mas de " + MAX_TICKETS_POR_LOTE + " tickets por lote");
        }
        return limpios;
    }

    private String motivo(ResponseStatusException excepcion) {
        String razon = excepcion.getReason();
        return razon != null ? razon : excepcion.getMessage();
    }

    private String validarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El titulo es obligatorio");
        }
        String limpio = titulo.trim();
        if (limpio.length() > LARGO_MAX_TITULO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El titulo no puede superar los " + LARGO_MAX_TITULO + " caracteres");
        }
        return limpio;
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > LARGO_MAX_DESCRIPCION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar los " + LARGO_MAX_DESCRIPCION + " caracteres");
        }
    }

    private void validarIdPositivo(Long id, String mensaje) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        }
    }

    private void validarCategoria(Long categoriaId) {
        if (!categorias.existsById(categoriaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No existe la categoria con id " + categoriaId);
        }
    }

    private String valor(Object valor) {
        if (valor == null) {
            return "";
        }
        return valor.toString();
    }

    private String csv(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace('\r', ' ').replace('\n', ' ');
        if (limpio.contains(",") || limpio.contains("\"")) {
            return "\"" + limpio.replace("\"", "\"\"") + "\"";
        }
        return limpio;
    }
}
