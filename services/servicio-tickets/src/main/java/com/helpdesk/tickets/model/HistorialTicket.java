package com.helpdesk.tickets.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bitacora de cambios de un ticket. Se guarda por id (sin relacion JPA) para
 * mantener el historial incluso si el ticket se elimina.
 */
@Entity
@Table(name = "historial_tickets")
public class HistorialTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(nullable = false, length = 40)
    private String campo;

    @Column(name = "valor_anterior", length = 500)
    private String valorAnterior;

    @Column(name = "valor_nuevo", length = 500)
    private String valorNuevo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false)
    private Instant fecha;

    public HistorialTicket() {
    }

    public HistorialTicket(Long ticketId, String campo, String valorAnterior, String valorNuevo,
                           Long usuarioId, Instant fecha) {
        this.ticketId = ticketId;
        this.campo = campo;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.usuarioId = usuarioId;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }

    public String getValorNuevo() { return valorNuevo; }
    public void setValorNuevo(String valorNuevo) { this.valorNuevo = valorNuevo; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Instant getFecha() { return fecha; }
    public void setFecha(Instant fecha) { this.fecha = fecha; }
}
