package com.helpdesk.tickets.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitudHoras(
        @NotNull @Positive Long tecnicoId,
        @Min(1) @Max(1440) int minutos,
        @Size(max = 500) String descripcion) {
}
