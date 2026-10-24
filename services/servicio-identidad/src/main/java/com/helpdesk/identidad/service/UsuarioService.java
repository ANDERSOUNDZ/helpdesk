package com.helpdesk.identidad.service;

import com.helpdesk.identidad.model.Usuario;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final Map<Long, Usuario> usuarios = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    public List<Usuario> obtenerTodos() {
        return usuarios.values().stream().sorted(Comparator.comparing(Usuario::getId)).toList();
    }

    public Usuario obtenerPorId(Long id) {
        Usuario usuario = usuarios.get(id);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario con id " + id);
        }
        return usuario;
    }

    public Usuario crear(Usuario nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (nuevo.getRol() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        nuevo.setId(siguienteId.getAndIncrement());
        usuarios.put(nuevo.getId(), nuevo);
        return nuevo;
    }
}
