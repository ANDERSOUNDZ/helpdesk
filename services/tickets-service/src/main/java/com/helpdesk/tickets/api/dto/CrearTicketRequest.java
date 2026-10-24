package com.helpdesk.tickets.api.dto;

import com.helpdesk.tickets.domain.model.Prioridad;

// DTO de ENTRADA: define el contrato JSON del POST. Es un record porque es un simple
// contenedor de datos inmutable.
//
// Por que existe aparte de Ticket: el contrato publico de la API no debe cambiar cada
// vez que cambie el dominio, y el cliente no debe poder mandar campos como id o estado.
// Las validaciones (titulo vacio, etc.) NO estan aqui: viven en el dominio.
public record CrearTicketRequest(String titulo, String descripcion, Prioridad prioridad) {
}
