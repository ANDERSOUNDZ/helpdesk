package com.helpdesk.tickets.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Horas dedicadas por un tecnico a un ticket. Se guardan por id del ticket
 * (sin relacion JPA), igual que el resto de referencias entre servicios.
 */
@Entity
@Table(name = "registros_horas")
public class RegistroHoras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "tecnico_id", nullable = false)
    private Long tecnicoId;

    @Column(nullable = false)
    private int minutos;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    public RegistroHoras() {
    }

    public RegistroHoras(Long ticketId, Long tecnicoId, int minutos, String descripcion, Instant creadoEn) {
        this.ticketId = ticketId;
        this.tecnicoId = tecnicoId;
        this.minutos = minutos;
        this.descripcion = descripcion;
        this.creadoEn = creadoEn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }

    public int getMinutos() { return minutos; }
    public void setMinutos(int minutos) { this.minutos = minutos; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
}
