package com.helpdesk.comunicacion.dto;

import java.util.Map;

public record ResumenEncuestasResponse(
        long totalEncuestas,
        long respondidas,
        long pendientes,
        double promedio,
        Map<Integer, Long> distribucion
) {
}
