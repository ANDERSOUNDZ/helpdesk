package com.helpdesk.identidad.service;

import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private static final Pattern FORMATO_USUARIO = Pattern.compile("^[a-z0-9._-]{3,60}$");
    private static final int LARGO_MINIMO_CLAVE = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public List<Usuario> obtenerTodos() {
        return usuarios.findAll(Sort.by("id"));
    }

    public Usuario obtenerPorId(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario con id " + id));
    }

    public Usuario crear(Usuario nuevo) {
        if (nuevo.getNombre() == null || nuevo.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre es obligatorio");
        }
        if (nuevo.getRol() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol es obligatorio");
        }
        nuevo.setId(null);
        nuevo.setPasswordHash(null);
        nuevo.setActivo(true);
        aplicarCredenciales(nuevo, nuevo.getUsername(), nuevo.getClave(), null);
        return usuarios.save(nuevo);
    }

    public Usuario actualizar(Long id, Usuario datos) {
        Usuario usuario = obtenerPorId(id);
        if (datos.getNombre() != null && !datos.getNombre().isBlank()) {
            usuario.setNombre(datos.getNombre());
        }
        if (datos.getRol() != null) {
            usuario.setRol(datos.getRol());
        }
        if (datos.getActivo() != null) {
            usuario.setActivo(datos.getActivo());
        }
        aplicarCredenciales(usuario, datos.getUsername(), datos.getClave(), usuario.getId());
        return usuarios.save(usuario);
    }

    public void eliminar(Long id) {
        Usuario usuario = obtenerPorId(id);
        usuarios.delete(usuario);
    }

    private void aplicarCredenciales(Usuario destino, String username, String clave, Long idActual) {
        if (username != null && !username.isBlank()) {
            String normalizado = username.trim().toLowerCase(Locale.ROOT);
            if (!FORMATO_USUARIO.matcher(normalizado).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El usuario debe tener de 3 a 60 caracteres: letras minusculas, numeros, punto, guion o guion bajo");
            }
            usuarios.findByUsername(normalizado)
                    .filter(existente -> idActual == null || !existente.getId().equals(idActual))
                    .ifPresent(existente -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre de acceso");
                    });
            destino.setUsername(normalizado);
        }
        if (clave != null && !clave.isBlank()) {
            if (destino.getUsername() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Para definir una clave hay que indicar el usuario (username)");
            }
            if (clave.length() < LARGO_MINIMO_CLAVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La clave debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
            }
            destino.setPasswordHash(codificador.encode(clave));
        }
        destino.setClave(null);
    }
}
