package com.helpdesk.identity.controller;

import com.helpdesk.identity.model.Rol;
import com.helpdesk.identity.model.Usuario;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsuarioControllerTest {

    private final UsuarioController controller = new UsuarioController();

    private Usuario usuario(String nombre, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setRol(rol);
        return u;
    }

    @Test
    void crearAsignaIdYSePuedeConsultar() {
        Usuario creado = controller.crear(usuario("Ana", Rol.ESTUDIANTE)).getBody();

        assertEquals(1L, creado.getId());
        assertEquals("Ana", controller.obtener(1L).getNombre());
        assertEquals(1, controller.listar().size());
    }

    @Test
    void rechazaNombreVacioORolNulo() {
        assertThrows(ResponseStatusException.class, () -> controller.crear(usuario(" ", Rol.TECNICO)));
        assertThrows(ResponseStatusException.class, () -> controller.crear(usuario("Luis", null)));
    }

    @Test
    void obtenerUnIdInexistenteDa404() {
        assertThrows(ResponseStatusException.class, () -> controller.obtener(99L));
    }
}
