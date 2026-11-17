package com.helpdesk.tickets.dto;

import com.helpdesk.tickets.model.RegistroHoras;

import java.util.List;

public record ResumenHoras(
        Long ticketId,
        long totalMinutos,
        double totalHoras,
        List<RegistroHoras> registros) {
}
