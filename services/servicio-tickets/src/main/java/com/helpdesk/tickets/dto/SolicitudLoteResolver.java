package com.helpdesk.tickets.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resuelve varios tickets a la vez. Si uno falla, los demas siguen.
 */
public record SolicitudLoteResolver(
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> ticketIds) {
}
