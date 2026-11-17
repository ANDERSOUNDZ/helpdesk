package com.helpdesk.tickets.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Prioridad prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoTicket estado = EstadoTicket.ABIERTO;

    @Column(name = "solicitante_id", nullable = false)
    private Long solicitanteId;

    @Column(name = "tecnico_id")
    private Long tecnicoId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    /** Fechas del ciclo de vida: permiten medir tiempos y SLA. */
    @Column(name = "creado_en")
    private Instant creadoEn;

    @Column(name = "asignado_en")
    private Instant asignadoEn;

    @Column(name = "resuelto_en")
    private Instant resueltoEn;

    @Column(name = "fecha_limite")
    private Instant fechaLimite;

    /** Evita repetir el recordatorio de SLA en cada ejecucion del job. */
    @Column(name = "recordatorio_enviado_en")
    private Instant recordatorioEnviadoEn;

    public Ticket() {
    }

    /** Campo calculado (no se guarda): el ticket esta vencido si sigue abierto y paso su fecha limite. */
    @Transient
    public boolean isVencido() {
        return fechaLimite != null
                && estado != EstadoTicket.RESUELTO
                && fechaLimite.isBefore(Instant.now());
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

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }

    public Instant getAsignadoEn() { return asignadoEn; }
    public void setAsignadoEn(Instant asignadoEn) { this.asignadoEn = asignadoEn; }

    public Instant getResueltoEn() { return resueltoEn; }
    public void setResueltoEn(Instant resueltoEn) { this.resueltoEn = resueltoEn; }

    public Instant getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(Instant fechaLimite) { this.fechaLimite = fechaLimite; }

    public Instant getRecordatorioEnviadoEn() { return recordatorioEnviadoEn; }
    public void setRecordatorioEnviadoEn(Instant recordatorioEnviadoEn) { this.recordatorioEnviadoEn = recordatorioEnviadoEn; }
}
