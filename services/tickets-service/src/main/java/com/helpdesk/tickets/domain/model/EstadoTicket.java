package com.helpdesk.tickets.domain.model;

// Enum en vez de String: el compilador impide estados inventados ("cerado", "Closed"...).
// Estos tres valores son parte del contrato con los demas servicios (analytics los cuenta,
// communication avisa cuando cambian). Agregar uno nuevo no los rompe: ellos leen el
// estado como texto y ignoran lo que no conocen.
public enum EstadoTicket {
    ABIERTO,
    EN_PROGRESO,
    RESUELTO
}
