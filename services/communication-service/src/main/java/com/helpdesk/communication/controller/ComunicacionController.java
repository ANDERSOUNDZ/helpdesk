package com.helpdesk.communication.controller;

import com.helpdesk.communication.model.Comentario;
import com.helpdesk.communication.model.Notificacion;
import com.helpdesk.communication.model.TicketEstadoCambiado;
import com.helpdesk.communication.service.ComunicacionService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/comentarios")
    public ResponseEntity<Comentario> crearComentario(@RequestBody Comentario nuevoComentario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comunicacionService.crearComentario(nuevoComentario));
    }

    @GetMapping("/notificaciones")
    public List<Notificacion> obtenerNotificaciones(@RequestParam(required = false) Long usuarioId,
                                                    @RequestParam(required = false) Boolean leida) {
        return comunicacionService.obtenerNotificaciones(usuarioId, leida);
    }

    @PutMapping("/notificaciones/{id}/leida")
    public Notificacion marcarNotificacionLeida(@PathVariable Long id) {
        return comunicacionService.marcarLeida(id);
    }

    @PostMapping("/eventos/ticket-estado")
    public ResponseEntity<Notificacion> recibirCambioDeEstado(@RequestBody TicketEstadoCambiado evento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comunicacionService.alCambiarEstado(evento));
    }
}
