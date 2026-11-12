package com.helpdesk.comunicacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comentarios", indexes = {
        @Index(name = "idx_comentarios_ticket", columnList = "ticket_id")
})
public class Comentario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "autor_nombre", length = 120)
    private String autorNombre;

    @Column(name = "ticket_titulo", length = 200)
    private String ticketTitulo;

    @Column(nullable = false, length = 1000)
    private String texto;

    @Column(name = "creado_en")
    private Instant creadoEn;

    public Comentario() {
    }

    @PrePersist
    void asignarFechaCreacion() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getAutorId() { return autorId; }
    public void setAutorId(Long autorId) { this.autorId = autorId; }

    public String getAutorNombre() { return autorNombre; }
    public void setAutorNombre(String autorNombre) { this.autorNombre = autorNombre; }

    public String getTicketTitulo() { return ticketTitulo; }
    public void setTicketTitulo(String ticketTitulo) { this.ticketTitulo = ticketTitulo; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}
