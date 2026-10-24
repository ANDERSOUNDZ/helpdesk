package com.helpdesk.identity.domain.model;

// Entidad de DOMINIO: Java puro, sin @Entity ni anotaciones de Spring, para que las
// reglas se prueben sin levantar nada y la base de datos se pueda cambiar sin tocarla.
//
// Es deliberadamente corta (id, nombre, rol). Credenciales y permisos se agregan cuando
// se implemente el login; aqui solo esta lo que los demas servicios necesitan referenciar.
// Los demas servicios guardan unicamente el id de un usuario, nunca esta clase.
public class Usuario {

    private final Long id;
    private final String nombre;
    private final Rol rol;

    // Privado: se crea solo por las fabricas, que dejan claro si es nuevo o ya existia.
    private Usuario(Long id, String nombre, Rol rol) {
        // La validacion vive en el dominio para que ninguna entrada futura pueda
        // crear un usuario invalido.
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (rol == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        this.id = id;
        this.nombre = nombre.trim();
        this.rol = rol;
    }

    // Usuario nuevo: sin id (lo asigna quien lo guarda).
    public static Usuario nuevo(String nombre, Rol rol) {
        return new Usuario(null, nombre, rol);
    }

    // Usuario que ya existia: lo usa el repositorio para reconstruirlo.
    public static Usuario reconstruir(Long id, String nombre, Rol rol) {
        return new Usuario(id, nombre, rol);
    }

    // Regla de negocio de permisos, aqui y no en el controller: solo un tecnico o un
    // administrador puede atender tickets. tickets-service no pregunta esto en cada
    // peticion (si identity cae, no deberia parar); se resolvera con el token de sesion.
    public boolean puedeAtenderTickets() {
        return rol == Rol.TECNICO || rol == Rol.ADMINISTRADOR;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public Rol getRol() { return rol; }
}
