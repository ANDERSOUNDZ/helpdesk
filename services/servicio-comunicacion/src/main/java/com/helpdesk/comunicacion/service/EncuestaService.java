package com.helpdesk.comunicacion.service;

import com.helpdesk.comunicacion.dto.ResumenEncuestasResponse;
import com.helpdesk.comunicacion.dto.RespuestaEncuestaRequest;
import com.helpdesk.comunicacion.model.EncuestaSatisfaccion;
import com.helpdesk.comunicacion.repository.EncuestaSatisfaccionRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EncuestaService {

    private final EncuestaSatisfaccionRepository encuestas;

    public EncuestaService(EncuestaSatisfaccionRepository encuestas) {
        this.encuestas = encuestas;
    }

    @Transactional
    public Optional<EncuestaSatisfaccion> crearSiNoExiste(Long ticketId, Long solicitanteId) {
        if (encuestas.existsByTicketId(ticketId)) {
            return Optional.empty();
        }
        EncuestaSatisfaccion encuesta = new EncuestaSatisfaccion();
        encuesta.setTicketId(ticketId);
        encuesta.setSolicitanteId(solicitanteId);
        encuesta.setCreadaEn(Instant.now());
        return Optional.of(encuestas.save(encuesta));
    }

    public EncuestaSatisfaccion obtenerPorTicket(Long ticketId) {
        return encuestas.findByTicketId(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe encuesta para el ticket " + ticketId));
    }

    @Transactional
    public EncuestaSatisfaccion responder(Long ticketId, RespuestaEncuestaRequest respuesta) {
        if (respuesta == null || respuesta.puntuacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuacion es obligatoria");
        }
        if (respuesta.puntuacion() < 1 || respuesta.puntuacion() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La puntuacion debe estar entre 1 y 5");
        }
        if (respuesta.comentario() != null && respuesta.comentario().length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede superar 500 caracteres");
        }

        EncuestaSatisfaccion encuesta = obtenerPorTicket(ticketId);
        if (encuesta.isRespondida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La encuesta del ticket ya fue respondida");
        }

        encuesta.setPuntuacion(respuesta.puntuacion());
        encuesta.setComentario(respuesta.comentario());
        encuesta.setRespondidaEn(Instant.now());
        return encuestas.save(encuesta);
    }

    public ResumenEncuestasResponse resumen() {
        long total = encuestas.count();
        long respondidas = encuestas.countByRespondidaEnIsNotNull();
        Double promedio = encuestas.promedio();

        Map<Integer, Long> distribucion = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribucion.put(i, 0L);
        }
        List<Object[]> filas = encuestas.distribucion();
        for (Object[] fila : filas) {
            Integer puntuacion = ((Number) fila[0]).intValue();
            Long cantidad = ((Number) fila[1]).longValue();
            distribucion.put(puntuacion, cantidad);
        }

        double promedioRedondeado = promedio == null
                ? 0.0
                : Math.round(promedio * 100.0) / 100.0;

        return new ResumenEncuestasResponse(
                total,
                respondidas,
                total - respondidas,
                promedioRedondeado,
                distribucion
        );
    }
}
