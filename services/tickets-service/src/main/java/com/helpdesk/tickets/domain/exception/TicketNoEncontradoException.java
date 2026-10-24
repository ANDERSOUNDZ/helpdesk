package com.helpdesk.tickets.domain.exception;

// Excepcion de dominio: dice QUE paso en terminos de negocio, sin hablar de HTTP.
// La capa api es la que decide que esto se traduce a un 404 (ver GlobalExceptionHandler).
public class TicketNoEncontradoException extends RuntimeException {

    public TicketNoEncontradoException(Long id) {
        super("No existe el ticket con id " + id);
    }
}
