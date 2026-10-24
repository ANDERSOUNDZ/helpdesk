package com.helpdesk.tickets.domain.model;

// Enum en vez de String: el compilador impide estados inventados ("cerado", "Closed"...).
public enum EstadoTicket {
    ABIERTO,
    CERRADO
}
