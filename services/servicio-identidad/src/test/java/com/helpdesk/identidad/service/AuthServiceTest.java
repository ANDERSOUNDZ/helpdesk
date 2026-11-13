package com.helpdesk.identidad.service;

import com.helpdesk.identidad.dto.LoginRequest;
import com.helpdesk.identidad.dto.LoginResponse;
import com.helpdesk.identidad.model.Rol;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres";

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final PasswordEncoder codificador = new BCryptPasswordEncoder();
    private final JwtService jwt = new JwtService(SECRETO, 60);
    private AuthService auth;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        ana = new Usuario();
        ana.setId(7L);
        ana.setNombre("Ana Lopez");
        ana.setRol(Rol.TECNICO);
        ana.setUsername("ana");
        ana.setPasswordHash(codificador.encode("secreta123"));
        when(repositorio.findByUsername(anyString())).thenAnswer(llamada ->
                "ana".equals(llamada.getArgument(0)) ? Optional.of(ana) : Optional.empty());
        auth = new AuthService(repositorio, codificador, jwt);
    }

    private HttpStatus estadoDe(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    private Jwt decodificar(String token) {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(token);
    }

    @Test
    void loginCorrectoEmiteUnTokenConElRolYElUsuario() {
        LoginResponse respuesta = auth.login(new LoginRequest("Ana", "secreta123"));

        assertEquals("Bearer", respuesta.tipo());
        assertEquals(Rol.TECNICO, respuesta.usuario().rol());
        Jwt token = decodificar(respuesta.token());
        assertEquals("ana", token.getSubject());
        assertEquals(JwtService.EMISOR, token.getClaimAsString("iss"));
        assertEquals(java.util.List.of("TECNICO"), token.getClaimAsStringList("roles"));
        assertEquals(7L, ((Number) token.getClaim("uid")).longValue());
        assertTrue(token.getExpiresAt().isAfter(token.getIssuedAt()));
    }

    @Test
    void elTokenCaducadoOConOtroEmisorEsRechazado() {
        LoginResponse respuesta = auth.login(new LoginRequest("ana", "secreta123"));
        NimbusJwtDecoder exigente = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        exigente.setJwtValidator(JwtValidators.createDefaultWithIssuer("otro-emisor"));

        assertThrows(RuntimeException.class, () -> exigente.decode(respuesta.token()));
    }

    @Test
    void claveIncorrectaDa401() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> auth.login(new LoginRequest("ana", "equivocada")));

        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
    }

    @Test
    void usuarioInexistenteDa401ConElMismoMensajeQueUnaClaveIncorrecta() {
        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class,
                () -> auth.login(new LoginRequest("nadie", "secreta123")));
        ResponseStatusException incorrecta = assertThrows(ResponseStatusException.class,
                () -> auth.login(new LoginRequest("ana", "equivocada")));

        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(inexistente));
        assertEquals(incorrecta.getReason(), inexistente.getReason());
    }

    @Test
    void usuarioInactivoDa401AunqueLaClaveSeaCorrecta() {
        ana.setActivo(false);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> auth.login(new LoginRequest("ana", "secreta123")));

        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
    }

    @Test
    void usuarioSinClaveDefinidaNoPuedeEntrar() {
        ana.setPasswordHash(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> auth.login(new LoginRequest("ana", "secreta123")));

        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
    }

    @Test
    void camposVaciosDan400() {
        assertEquals(HttpStatus.BAD_REQUEST,
                estadoDe(assertThrows(ResponseStatusException.class, () -> auth.login(new LoginRequest(" ", "x")))));
        assertEquals(HttpStatus.BAD_REQUEST,
                estadoDe(assertThrows(ResponseStatusException.class, () -> auth.login(new LoginRequest("ana", "")))));
        assertEquals(HttpStatus.BAD_REQUEST,
                estadoDe(assertThrows(ResponseStatusException.class, () -> auth.login(null))));
    }

    @Test
    void sinSecretoConfiguradoElLoginDa503() {
        AuthService sinSecreto = new AuthService(repositorio, codificador, new JwtService("corto", 60));

        assertFalse(new JwtService("corto", 60).estaConfigurado());
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> sinSecreto.login(new LoginRequest("ana", "secreta123")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, estadoDe(e));
    }
}
