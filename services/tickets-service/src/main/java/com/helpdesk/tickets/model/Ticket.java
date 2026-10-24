package com.helpdesk.tickets.model;

// Entidad simple, como Espacio en espacios-service: campos, constructor vacio y getters/setters.
// El constructor vacio es lo que permite que Spring convierta el JSON de Postman en un Ticket.
public class Ticket {

    private Long id;
    private String titulo;
    private String descripcion;
    private Prioridad prioridad;
    private EstadoTicket estado = EstadoTicket.ABIERTO;
    // Solo el ID de las personas, no el usuario completo: ese dato es de identity-service.
    // Asi tickets sigue funcionando aunque identity este caido.
    private Long solicitanteId;
    private Long tecnicoId;

    public Ticket() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public EstadoTicket getEstado() { return estado; }
    public void setEstado(EstadoTicket estado) { this.estado = estado; }

    public Long getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(Long solicitanteId) { this.solicitanteId = solicitanteId; }

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }
}
