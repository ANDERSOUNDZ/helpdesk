package com.helpdesk.tickets.api.error;

import com.helpdesk.tickets.domain.exception.TicketNoEncontradoException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Un unico lugar traduce excepciones a respuestas HTTP. El dominio lanza excepciones
// "de negocio" y no sabe que es un 404; el controller no hace try/catch. Si manana
// cambia el formato de error, se cambia solo aqui.
//
// ProblemDetail es el formato estandar de errores HTTP (RFC 9457): todos los servicios
// del equipo responderan los errores con la misma forma.
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketNoEncontradoException.class)
    public ProblemDetail noEncontrado(TicketNoEncontradoException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // El dominio rechaza datos invalidos con IllegalArgumentException (titulo vacio,
    // prioridad nula...). Para el cliente eso es un error suyo: 400, no 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail datosInvalidos(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Los datos son validos pero chocan con el estado actual del ticket (por ejemplo,
    // asignar uno ya resuelto): eso es un 409 Conflict, no un error del cliente ni del servidor.
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail conflicto(IllegalStateException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
