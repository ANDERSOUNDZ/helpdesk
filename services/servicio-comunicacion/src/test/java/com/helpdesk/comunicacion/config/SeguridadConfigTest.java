package com.helpdesk.comunicacion.config;

import com.helpdesk.comunicacion.controller.ComunicacionController;
import com.helpdesk.comunicacion.controller.ManejadorErrores;
import com.helpdesk.comunicacion.service.AdjuntoService;
import com.helpdesk.comunicacion.service.ComunicacionService;
import com.helpdesk.comunicacion.service.EncuestaService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComunicacionController.class)
@Import({SeguridadConfig.class, ManejadorErrores.class})
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.jwt.secret=una-clave-de-prueba-de-al-menos-32-caracteres"
})
class SeguridadConfigTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ComunicacionService comunicacionService;

    @MockitoBean
    private EncuestaService encuestaService;

    @MockitoBean
    private AdjuntoService adjuntoService;

    private RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private String token(String rol, String secreto, String emisor, long segundosDeVida) {
        Instant ahora = Instant.now();
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(emisor)
                .subject("ana")
                .issuedAt(ahora.minusSeconds(7200))
                .expiresAt(ahora.plusSeconds(segundosDeVida))
                .claim("uid", 1L)
                .claim("roles", List.of(rol))
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256")))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), datos))
                .getTokenValue();
    }

    @Test
    void sinTokenNadaEsAccesibleSalvoLaSalud() throws Exception {
        mvc.perform(get("/comentarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/encuestas/resumen")).andExpect(status().isUnauthorized());
        int salud = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(401, salud);
    }

    @Test
    void cualquierRolAutenticadoPuedeLeerComentariosYNotificaciones() throws Exception {
        for (String rol : List.of("SOLICITANTE", "TECNICO", "ADMINISTRADOR")) {
            mvc.perform(get("/comentarios").with(como(rol))).andExpect(status().isOk());
            mvc.perform(get("/notificaciones").with(como(rol))).andExpect(status().isOk());
        }
    }

    @Test
    void soloElAdministradorPuedeEliminarComentariosYAdjuntos() throws Exception {
        for (String rol : List.of("SOLICITANTE", "TECNICO")) {
            mvc.perform(delete("/comentarios/1").with(como(rol))).andExpect(status().isForbidden());
            mvc.perform(delete("/adjuntos/1").with(como(rol))).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/comentarios/1").with(como("ADMINISTRADOR"))).andExpect(status().isNoContent());
        mvc.perform(delete("/adjuntos/1").with(como("ADMINISTRADOR"))).andExpect(status().isNoContent());
    }

    @Test
    void unTokenRealFirmadoConLaClaveEsAceptadoYRespetaElRol() throws Exception {
        String solicitante = token("SOLICITANTE", SECRETO, "helpdesk-identidad", 3600);
        String admin = token("ADMINISTRADOR", SECRETO, "helpdesk-identidad", 3600);

        mvc.perform(get("/comentarios").header("Authorization", "Bearer " + solicitante)).andExpect(status().isOk());
        mvc.perform(delete("/comentarios/1").header("Authorization", "Bearer " + solicitante))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/comentarios/1").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
    }

    @Test
    void seRechazanTokensConOtraClaveVencidosDeOtroEmisorOMalFormados() throws Exception {
        String otraClave = token("ADMINISTRADOR", "otra-clave-distinta-de-al-menos-32-caracteres!", "helpdesk-identidad", 3600);
        String vencido = token("ADMINISTRADOR", SECRETO, "helpdesk-identidad", -3600);
        String otroEmisor = token("ADMINISTRADOR", SECRETO, "otro-emisor", 3600);

        for (String malo : List.of(otraClave, vencido, otroEmisor, "basura")) {
            mvc.perform(get("/comentarios").header("Authorization", "Bearer " + malo)).andExpect(status().isUnauthorized());
        }
    }
}
