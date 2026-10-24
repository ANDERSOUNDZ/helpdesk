package com.helpdesk.communication.domain.model;

// Entidad de DOMINIO: un aviso para un usuario ("tu ticket fue resuelto").
// Guarda solo el id del usuario destinatario, igual que Comentario: no conoce a
// identity-service.
public class Notificacion {

    private final Long id;
    private final Long usuarioId;
    private final String mensaje;
    private boolean leida;

    private Notificacion(Long id, Long usuarioId, String mensaje, boolean leida) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("El destinatario es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje es obligatorio");
        }
        this.id = id;
        this.usuarioId = usuarioId;
        this.mensaje = mensaje.trim();
        this.leida = leida;
    }

    // Toda notificacion nace sin leer.
    public static Notificacion nueva(Long usuarioId, String mensaje) {
        return new Notificacion(null, usuarioId, mensaje, false);
    }

    // Notificacion que ya existia: lo usa el repositorio para reconstruirla.
    public static Notificacion reconstruir(Long id, Long usuarioId, String mensaje, boolean leida) {
        return new Notificacion(id, usuarioId, mensaje, leida);
    }

    // Metodo de negocio y no un setLeida(): una notificacion leida no vuelve a "sin leer".
    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public String getMensaje() { return mensaje; }
    public boolean isLeida() { return leida; }
}
