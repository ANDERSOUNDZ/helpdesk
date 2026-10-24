package com.helpdesk.tickets.domain.model;

// Entidad de DOMINIO: Java puro, sin @Entity ni anotaciones de Spring.
// Asi las reglas del negocio se prueban sin levantar nada, y cambiar de base de datos
// (o de framework) no obliga a tocar esta clase. En espacios-service el modelo y la
// tabla eran la misma clase; aqui se separan a proposito.
public class Ticket {

    private final Long id;
    private final String titulo;
    private final String descripcion;
    private final Prioridad prioridad;
    private EstadoTicket estado;

    // Constructor privado: solo se crea por las dos fabricas de abajo, que dejan claro
    // si el ticket es nuevo o viene de la base de datos.
    private Ticket(Long id, String titulo, String descripcion, Prioridad prioridad, EstadoTicket estado) {
        // La validacion vive en el dominio (no en el controller) para que ninguna otra
        // entrada futura (eventos, otro endpoint) pueda crear un ticket invalido.
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        if (prioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }
        this.id = id;
        this.titulo = titulo.trim();
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = estado;
    }

    // Ticket nuevo: sin id (lo asigna quien lo guarda) y siempre nace ABIERTO.
    public static Ticket nuevo(String titulo, String descripcion, Prioridad prioridad) {
        return new Ticket(null, titulo, descripcion, prioridad, EstadoTicket.ABIERTO);
    }

    // Ticket que ya existia: lo usa el repositorio para reconstruirlo desde el almacenamiento.
    public static Ticket reconstruir(Long id, String titulo, String descripcion,
                                     Prioridad prioridad, EstadoTicket estado) {
        return new Ticket(id, titulo, descripcion, prioridad, estado);
    }

    // La regla "cerrar" es del negocio, por eso es un metodo aqui y no un setEstado()
    // publico: nadie puede dejar el ticket en un estado que el negocio no permite.
    public void cerrar() {
        this.estado = EstadoTicket.CERRADO;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public EstadoTicket getEstado() { return estado; }
}
