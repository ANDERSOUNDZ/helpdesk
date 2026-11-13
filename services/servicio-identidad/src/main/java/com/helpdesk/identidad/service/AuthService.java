package com.helpdesk.identidad.service;

import com.helpdesk.identidad.dto.LoginRequest;
import com.helpdesk.identidad.dto.LoginResponse;
import com.helpdesk.identidad.dto.UsuarioSesion;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;
    private final JwtService jwt;
    private final String hashFalso;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder codificador, JwtService jwt) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.jwt = jwt;
        this.hashFalso = codificador.encode("clave-inexistente");
    }

    public LoginResponse login(LoginRequest pedido) {
        if (pedido == null || pedido.username() == null || pedido.username().isBlank()
                || pedido.clave() == null || pedido.clave().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario y la clave son obligatorios");
        }
        if (!jwt.estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticacion no configurada: falta JWT_SECRET (minimo " + JwtService.LARGO_MINIMO_SECRETO + " caracteres)");
        }

        Usuario usuario = usuarios.findByUsername(pedido.username().trim().toLowerCase(Locale.ROOT)).orElse(null);
        String hash = usuario != null && usuario.getPasswordHash() != null ? usuario.getPasswordHash() : hashFalso;
        boolean claveCorrecta = codificador.matches(pedido.clave(), hash);

        if (usuario == null || !claveCorrecta || !usuario.estaActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

        JwtService.TokenEmitido emitido = jwt.emitir(usuario);
        return new LoginResponse(emitido.token(), "Bearer", emitido.expiraEn().getEpochSecond(),
                new UsuarioSesion(usuario.getId(), usuario.getNombre(), usuario.getUsername(), usuario.getRol()));
    }
}
