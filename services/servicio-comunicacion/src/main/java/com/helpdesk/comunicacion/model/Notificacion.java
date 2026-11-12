package com.helpdesk.comunicacion.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notificaciones", indexes = {
        @Index(name = "idx_notificaciones_usuario_leida", columnList = "usuario_id,leida")
})
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TipoNotificacion tipo;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(name = "creada_en")
    private Instant creadaEn;

    @Column(name = "leida_en")
    private Instant leidaEn;

    public Notificacion() {
    }

    @PrePersist
    void asignarFechaCreacion() {
        if (creadaEn == null) {
            creadaEn = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    public TipoNotificacion getTipo() {
        return tipo == null ? TipoNotificacion.SISTEMA : tipo;
    }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getReferenciaId() { return referenciaId; }
    public void setReferenciaId(Long referenciaId) { this.referenciaId = referenciaId; }

    public Instant getCreadaEn() { return creadaEn; }
    public void setCreadaEn(Instant creadaEn) { this.creadaEn = creadaEn; }

    public Instant getLeidaEn() { return leidaEn; }
    public void setLeidaEn(Instant leidaEn) { this.leidaEn = leidaEn; }
}
