package com.helpdesk.identity.controller;

import com.helpdesk.identity.model.Usuario;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Un solo controller con los datos en memoria (se pierden al reiniciar): la version mas
// simple posible para probar con Postman. ConcurrentHashMap/AtomicLong porque Spring
// atiende varias peticiones a la vez.
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final Map<Long, Usuario> usuarios = new ConcurrentHashMap<>();
    private final AtomicLong siguienteId = new AtomicLong(1);

    @GetMapping
    public List<Usuario> listar() {
        return usuarios.values().stream().sorted(Comparator.comparing(Usuario::getId)).toList();
    }

    @GetMapping("/{id}")
    public Usuario obtener(@PathVariable Long id) {
        Usuario usuario = usuarios.get(id);
        if (usuario == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario con id " + id);
        }
        return usuario;
    }

    @PostMapping
    public ResponseEntity<Usuario> crear(@RequestBody Usuario nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (nuevo.getRol() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        // El id lo decide el servidor, no el cliente.
        nuevo.setId(siguienteId.getAndIncrement());
        usuarios.put(nuevo.getId(), nuevo);
        return ResponseEntity.created(URI.create("/usuarios/" + nuevo.getId())).body(nuevo);
    }
}
