package com.helpdesk.estadisticas.config;

import com.helpdesk.estadisticas.controller.EstadisticasController;
import com.helpdesk.estadisticas.model.ContadorEstado;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.service.EstadisticasService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EstadisticasController.class)
@Import(SeguridadConfig.class)
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.jwt.secret=una-clave-de-prueba-de-al-menos-32-caracteres"
})
class SeguridadConfigTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres";
    private static final String EVENTO = "{\"ticketId\":1,\"estadoAnterior\":\"ABIERTO\",\"estadoNuevo\":\"RESUELTO\",\"solicitanteId\":3}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstadisticasService estadisticasService;

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

    private void preparar() {
        ResumenTickets resumen = new ResumenTickets(1, 2, 3);
        when(estadisticasService.obtenerResumen()).thenReturn(resumen);
        when(estadisticasService.alCambiarEstado(any())).thenReturn(resumen);
        when(estadisticasService.obtenerTodos()).thenReturn(List.of(new ContadorEstado("ABIERTO", 1)));
        when(estadisticasService.obtenerPorEstado(anyString())).thenReturn(new ContadorEstado("ABIERTO", 1));
        when(estadisticasService.ajustarContador(anyString(), anyInt())).thenReturn(new ContadorEstado("ABIERTO", 3));
    }

    @Test
    void sinTokenNadaEsAccesibleSalvoLaSalud() throws Exception {
        mvc.perform(get("/resumen")).andExpect(status().isUnauthorized());
        mvc.perform(post("/eventos/ticket-estado").contentType(MediaType.APPLICATION_JSON).content(EVENTO))
                .andExpect(status().isUnauthorized());
        int salud = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(401, salud);
    }

    @Test
    void lasEstadisticasLasVenElTecnicoYElAdministradorPeroNoElSolicitante() throws Exception {
        preparar();

        for (String ruta : List.of("/resumen", "/estadisticas/estados", "/estadisticas/estados/ABIERTO")) {
            mvc.perform(get(ruta).with(como("SOLICITANTE"))).andExpect(status().isForbidden());
            mvc.perform(get(ruta).with(como("TECNICO"))).andExpect(status().isOk());
            mvc.perform(get(ruta).with(como("ADMINISTRADOR"))).andExpect(status().isOk());
        }
    }

    @Test
    void cualquierUsuarioAutenticadoPuedeEnviarElEventoDeCambioDeEstado() throws Exception {
        preparar();

        for (String rol : List.of("SOLICITANTE", "TECNICO", "ADMINISTRADOR")) {
            mvc.perform(post("/eventos/ticket-estado").with(como(rol)).contentType(MediaType.APPLICATION_JSON).content(EVENTO))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void ajustarYResetearSonSoloDelAdministrador() throws Exception {
        preparar();

        for (String rol : List.of("SOLICITANTE", "TECNICO")) {
            mvc.perform(put("/estadisticas/estados/ABIERTO").param("cantidad", "3").with(como(rol)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/estadisticas/reset").with(como(rol))).andExpect(status().isForbidden());
        }
        mvc.perform(put("/estadisticas/estados/ABIERTO").param("cantidad", "3").with(como("ADMINISTRADOR")))
                .andExpect(status().isOk());
        mvc.perform(delete("/estadisticas/reset").with(como("ADMINISTRADOR"))).andExpect(status().isNoContent());
    }

    @Test
    void unTokenRealFirmadoConLaClaveEsAceptadoYRespetaElRol() throws Exception {
        preparar();
        String solicitante = token("SOLICITANTE", SECRETO, "helpdesk-identidad", 3600);
        String tecnico = token("TECNICO", SECRETO, "helpdesk-identidad", 3600);

        mvc.perform(get("/resumen").header("Authorization", "Bearer " + solicitante)).andExpect(status().isForbidden());
        mvc.perform(get("/resumen").header("Authorization", "Bearer " + tecnico)).andExpect(status().isOk());
    }

    @Test
    void seRechazanTokensConOtraClaveVencidosDeOtroEmisorOMalFormados() throws Exception {
        String otraClave = token("ADMINISTRADOR", "otra-clave-distinta-de-al-menos-32-caracteres!", "helpdesk-identidad", 3600);
        String vencido = token("ADMINISTRADOR", SECRETO, "helpdesk-identidad", -3600);
        String otroEmisor = token("ADMINISTRADOR", SECRETO, "otro-emisor", 3600);

        for (String malo : List.of(otraClave, vencido, otroEmisor, "basura")) {
            mvc.perform(get("/resumen").header("Authorization", "Bearer " + malo)).andExpect(status().isUnauthorized());
        }
    }
}
