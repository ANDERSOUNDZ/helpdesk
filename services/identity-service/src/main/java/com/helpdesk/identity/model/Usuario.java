package com.helpdesk.identity.model;

// Entidad simple, como Espacio en espacios-service. Credenciales y permisos se agregan
// cuando se implemente el login; hoy solo lo que los demas servicios necesitan referenciar
// (ellos guardan unicamente el id del usuario, nunca esta clase).
public class Usuario {

    private Long id;
    private String nombre;
    private Rol rol;

    public Usuario() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}
