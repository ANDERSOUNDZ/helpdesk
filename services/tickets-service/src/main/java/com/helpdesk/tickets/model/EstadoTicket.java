package com.helpdesk.tickets.model;

// Enum y no String: el compilador impide estados inventados. Los demas servicios lo
// reciben como texto ("RESUELTO") y ignoran los estados que no conozcan.
public enum EstadoTicket {
    ABIERTO,
    EN_PROGRESO,
    RESUELTO
}
