package com.helpdesk.communication.domain.model;

// Entidad de DOMINIO: Java puro, sin @Entity ni anotaciones de Spring.
//
// Un comentario guarda SOLO los ids del ticket y del autor. Este servicio no es dueno de
// los tickets ni de los usuarios, asi que no los consulta para crear un comentario: si
// tickets-service o identity-service estan caidos, comentar sigue funcionando.
public class Comentario {

    private final Long id;
    private final Long ticketId;
    private final Long autorId;
    private final String texto;

    // Privado: se crea solo por las fabricas, que dejan claro si es nuevo o ya existia.
    private Comentario(Long id, Long ticketId, Long autorId, String texto) {
        // La validacion vive en el dominio para que ninguna entrada futura (evento,
        // otro endpoint) pueda crear un comentario invalido.
        if (ticketId == null) {
            throw new IllegalArgumentException("El ticket es obligatorio");
        }
        if (autorId == null) {
            throw new IllegalArgumentException("El autor es obligatorio");
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El texto es obligatorio");
        }
        this.id = id;
        this.ticketId = ticketId;
        this.autorId = autorId;
        this.texto = texto.trim();
    }

    // Comentario nuevo: sin id (lo asigna quien lo guarda).
    public static Comentario nuevo(Long ticketId, Long autorId, String texto) {
        return new Comentario(null, ticketId, autorId, texto);
    }

    // Comentario que ya existia: lo usa el repositorio para reconstruirlo.
    public static Comentario reconstruir(Long id, Long ticketId, Long autorId, String texto) {
        return new Comentario(id, ticketId, autorId, texto);
    }

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public Long getAutorId() { return autorId; }
    public String getTexto() { return texto; }
}
