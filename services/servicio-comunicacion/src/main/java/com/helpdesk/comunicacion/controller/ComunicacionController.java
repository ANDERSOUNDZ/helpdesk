package com.helpdesk.comunicacion.controller;

import com.helpdesk.comunicacion.dto.ContadorNotificacionesResponse;
import com.helpdesk.comunicacion.dto.CrearComentarioRequest;
import com.helpdesk.comunicacion.dto.PaginaComentariosResponse;
import com.helpdesk.comunicacion.model.Comentario;
import com.helpdesk.comunicacion.model.Notificacion;
import com.helpdesk.comunicacion.model.TicketEstadoCambiado;
import com.helpdesk.comunicacion.model.TipoNotificacion;
import com.helpdesk.comunicacion.service.ComunicacionService;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ComunicacionController {

    private final ComunicacionService comunicacionService;

    public ComunicacionController(ComunicacionService comunicacionService) {
        this.comunicacionService = comunicacionService;
    }

    @GetMapping("/comentarios")
    public List<Comentario> obtenerComentarios(@RequestParam(required = false) Long ticketId) {
        return comunicacionService.obtenerComentarios(ticketId);
    }

    @GetMapping("/comentarios/paginados")
    public PaginaComentariosResponse<Comentario> obtenerComentariosPaginados(
            @RequestParam(required = false) Long ticketId,
            @RequestParam(required = false) List<Long> ticketIds,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "creadoEn,desc") String sort) {
        return PaginaComentariosResponse.desde(
                comunicacionService.obtenerComentariosPaginados(
                        ticketId, ticketIds, q, construirPageable(page, size, sort)));
    }

    @PostMapping("/comentarios")
    public ResponseEntity<Comentario> crearComentario(@RequestBody CrearComentarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comunicacionService.crearComentario(request));
    }

    @DeleteMapping("/comentarios/{id}")
    public ResponseEntity<Void> eliminarComentario(@PathVariable Long id) {
        comunicacionService.eliminarComentario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> obtenerNotificaciones(
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Boolean leida,
            @RequestParam(required = false) TipoNotificacion tipo,
            @RequestParam(required = false) Long ticketId) {
        return comunicacionService.obtenerNotificaciones(usuarioId, leida, tipo, ticketId);
    }

    @GetMapping("/notificaciones/contador")
    public ContadorNotificacionesResponse contadorNotificaciones(
            @RequestParam Long usuarioId) {
        return comunicacionService.contadorNotificaciones(usuarioId);
    }

    @PutMapping("/notificaciones/leidas")
    public ResponseEntity<Void> marcarTodasLeidas(@RequestParam Long usuarioId) {
        comunicacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notificaciones/{id}/leida")
    public Notificacion marcarNotificacionLeida(@PathVariable Long id) {
        return comunicacionService.marcarLeida(id);
    }

    @PostMapping("/eventos/ticket-estado")
    public ResponseEntity<Notificacion> recibirCambioDeEstado(
            @RequestBody TicketEstadoCambiado evento) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comunicacionService.alCambiarEstado(evento));
    }

    private Pageable construirPageable(int page, int size, String sort) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page no puede ser negativo");
        }
        if (size < 1 || size > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size debe estar entre 1 y 50");
        }

        String[] partes = sort == null ? new String[]{"creadoEn", "desc"} : sort.split(",");
        String campo = partes.length > 0 ? partes[0].trim() : "creadoEn";
        if (!campo.equals("creadoEn") && !campo.equals("id")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sort solo permite creadoEn o id");
        }
        Sort.Direction direccion = partes.length > 1 && "asc".equalsIgnoreCase(partes[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort orden = Sort.by(direccion, campo);
        if (!campo.equals("id")) {
            orden = orden.and(Sort.by(direccion, "id"));
        }
        return PageRequest.of(page, size, orden);
    }
}
