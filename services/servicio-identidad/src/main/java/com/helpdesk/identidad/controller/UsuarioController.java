package com.helpdesk.identidad.controller;

import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.service.UsuarioService;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public List<Usuario> obtenerUsuarios() {
        return usuarioService.obtenerTodos();
    }

    @GetMapping("/usuarios/{id}")
    public Usuario obtenerUsuarioPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario nuevoUsuario) {
        Usuario creado = usuarioService.crear(nuevoUsuario);
        return ResponseEntity.created(URI.create("/usuarios/" + creado.getId())).body(creado);
    }

    @PutMapping("/usuarios/{id}")
    public Usuario actualizarUsuario(@PathVariable Long id, @RequestBody Usuario datos) {
        return usuarioService.actualizar(id, datos);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        usuarioService.eliminar(id, idDe(jwt));
        return ResponseEntity.noContent().build();
    }

    /** Id del usuario autenticado (claim "uid"); null si el login esta apagado o el token no lo trae. */
    private static Long idDe(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object uid = jwt.getClaims().get("uid");
        return uid instanceof Number numero ? Long.valueOf(numero.longValue()) : null;
    }
}
