package com.helpdesk.identity.service;

import com.helpdesk.identity.model.Rol;
import com.helpdesk.identity.model.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioServiceTest {

    private final UsuarioService service = new UsuarioService();

    private Usuario usuario(String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setRol(rol);
        return u;
    }

    @Test
    void crearAsignaIdYSePuedeConsultar() {
        Usuario creado = service.crear(usuario("Ana", Rol.ESTUDIANTE));

        assertEquals(1L, creado.getId());
        assertEquals("Ana", service.obtenerPorId(1L).getNombre());
        assertEquals(1, service.obtenerTodos().size());
    }

    @Test
    void rechazaNombreVacioORolNulo() {
        assertThrows(ResponseStatusException.class, () -> service.crear(usuario(" ", Rol.TECNICO)));
        assertThrows(ResponseStatusException.class, () -> service.crear(usuario("Luis", null)));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> service.obtenerPorId(99L));
    }
}
