package com.helpdesk.identidad.service;

import com.helpdesk.identidad.model.Rol;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final Map<Long, Usuario> baseDeDatos = new HashMap<>();
    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private UsuarioService service;

    @BeforeEach
    void preparar() {
        when(repositorio.save(any(Usuario.class))).thenAnswer(llamada -> {
            Usuario usuario = llamada.getArgument(0);
            if (usuario.getId() == null) {
                usuario.setId((long) baseDeDatos.size() + 1);
            }
            baseDeDatos.put(usuario.getId(), usuario);
            return usuario;
        });
        when(repositorio.findById(anyLong()))
                .thenAnswer(llamada -> Optional.ofNullable(baseDeDatos.get(llamada.<Long>getArgument(0))));
        when(repositorio.findAll(any(Sort.class))).thenAnswer(llamada -> new ArrayList<>(baseDeDatos.values()));
        service = new UsuarioService(repositorio);
    }

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
