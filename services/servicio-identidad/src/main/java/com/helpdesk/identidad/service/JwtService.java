package com.helpdesk.identidad.service;

import com.helpdesk.identidad.model.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JwtService {

    public static final String EMISOR = "helpdesk-identidad";
    public static final int LARGO_MINIMO_SECRETO = 32;

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    private final JwtEncoder codificador;
    private final long minutosDeVida;

    public JwtService(@Value("${app.jwt.secret:}") String secreto,
                      @Value("${app.jwt.expiracion-minutos:60}") long minutosDeVida) {
        this.minutosDeVida = minutosDeVida;
        byte[] clave = secreto == null ? new byte[0] : secreto.getBytes(StandardCharsets.UTF_8);
        this.codificador = clave.length >= LARGO_MINIMO_SECRETO
                ? new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(clave, "HmacSHA256")))
                : null;
    }

    public boolean estaConfigurado() {
        return codificador != null;
    }

    public TokenEmitido emitir(Usuario usuario) {
        if (!estaConfigurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Autenticacion no configurada: falta JWT_SECRET (minimo " + LARGO_MINIMO_SECRETO + " caracteres)");
        }
        Instant ahora = Instant.now();
        Instant vence = ahora.plus(minutosDeVida, ChronoUnit.MINUTES);
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim("uid", usuario.getId())
                .claim("nombre", usuario.getNombre())
                .claim("roles", List.of(usuario.getRol().name()))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
        return new TokenEmitido(token, vence);
    }
}
