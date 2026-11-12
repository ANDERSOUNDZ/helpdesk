package com.helpdesk.comunicacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "encuestas_satisfaccion")
public class EncuestaSatisfaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false, unique = true)
    private Long ticketId;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    private Integer puntuacion;

    @Column(length = 500)
    private String comentario;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "respondida_en")
    private Instant respondidaEn;

    public EncuestaSatisfaccion() {
    }

    @PrePersist
    void asignarFechaCreacion() {
        if (creadaEn == null) {
            creadaEn = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getSolicitanteId() { return solicitanteId; }
    public void setSolicitanteId(Long solicitanteId) { this.solicitanteId = solicitanteId; }

    public Integer getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Integer puntuacion) { this.puntuacion = puntuacion; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }

    public Instant getRespondidaEn() { return respondidaEn; }
    public void setRespondidaEn(Instant respondidaEn) { this.respondidaEn = respondidaEn; }

    public boolean isRespondida() {
        return respondidaEn != null;
    }
}
