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
    // Solo el ID de quien pidio el ticket, no el usuario completo: ese dato es de
    // identity-service. Asi tickets sigue funcionando aunque identity este caido.
    private final Long solicitanteId;
    private Long tecnicoId; // null mientras nadie lo haya tomado
    private EstadoTicket estado;

    // Constructor privado: solo se crea por las dos fabricas de abajo, que dejan claro
    // si el ticket es nuevo o viene de la base de datos.
    private Ticket(Long id, String titulo, String descripcion, Prioridad prioridad,
                   EstadoTicket estado, Long solicitanteId, Long tecnicoId) {
        // La validacion vive en el dominio (no en el controller) para que ninguna otra
        // entrada futura (eventos, otro endpoint) pueda crear un ticket invalido.
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El titulo es obligatorio");
        }
        if (prioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }
        if (solicitanteId == null) {
            throw new IllegalArgumentException("El solicitante es obligatorio");
        }
        this.id = id;
        this.titulo = titulo.trim();
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = estado;
        this.solicitanteId = solicitanteId;
        this.tecnicoId = tecnicoId;
    }

    // Ticket nuevo: sin id (lo asigna quien lo guarda), sin tecnico y siempre nace ABIERTO.
    public static Ticket nuevo(String titulo, String descripcion, Prioridad prioridad, Long solicitanteId) {
        return new Ticket(null, titulo, descripcion, prioridad, EstadoTicket.ABIERTO, solicitanteId, null);
    }

    // Ticket que ya existia: lo usa el repositorio para reconstruirlo desde el almacenamiento.
    public static Ticket reconstruir(Long id, String titulo, String descripcion, Prioridad prioridad,
                                     EstadoTicket estado, Long solicitanteId, Long tecnicoId) {
        return new Ticket(id, titulo, descripcion, prioridad, estado, solicitanteId, tecnicoId);
    }

    // Tomar el ticket lo pasa a EN_PROGRESO: asignar y empezar a trabajar son un solo paso
    // para el negocio, asi nunca queda un ticket con tecnico pero aun "ABIERTO".
    public void asignarA(Long tecnicoId) {
        if (tecnicoId == null) {
            throw new IllegalArgumentException("El tecnico es obligatorio");
        }
        // Un ticket resuelto no se reasigna: reabrirlo seria otra regla de negocio.
        if (estado == EstadoTicket.RESUELTO) {
            throw new IllegalStateException("No se puede asignar un ticket ya resuelto");
        }
        this.tecnicoId = tecnicoId;
        this.estado = EstadoTicket.EN_PROGRESO;
    }

    // La regla "resolver" es del negocio, por eso es un metodo aqui y no un setEstado()
    // publico: nadie puede dejar el ticket en un estado que el negocio no permite.
    public void resolver() {
        this.estado = EstadoTicket.RESUELTO;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Prioridad getPrioridad() { return prioridad; }
    public EstadoTicket getEstado() { return estado; }
    public Long getSolicitanteId() { return solicitanteId; }
    public Long getTecnicoId() { return tecnicoId; }
}
