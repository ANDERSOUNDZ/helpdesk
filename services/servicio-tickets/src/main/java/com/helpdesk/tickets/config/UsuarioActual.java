package com.helpdesk.tickets.config;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Obtiene el id del usuario autenticado (claim "uid" del JWT de identidad).
 * Con el login apagado devuelve vacio y el historial queda sin usuario.
 */
@Component
public class UsuarioActual {

    public Optional<Long> id() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        Object uid = jwt.getClaim("uid");
        if (uid instanceof Number numero) {
            return Optional.of(numero.longValue());
        }
        return Optional.empty();
    }
}
