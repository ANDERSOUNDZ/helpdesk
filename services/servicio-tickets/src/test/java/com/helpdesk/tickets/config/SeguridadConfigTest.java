package com.helpdesk.tickets.config;

import com.helpdesk.tickets.controller.TicketController;
import com.helpdesk.tickets.model.Prioridad;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.service.TicketService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(SeguridadConfig.class)
@TestPropertySource(properties = {
        "app.auth.enabled=true",
        "app.jwt.secret=una-clave-de-prueba-de-al-menos-32-caracteres"
})
class SeguridadConfigTest {

    private static final String SECRETO = "una-clave-de-prueba-de-al-menos-32-caracteres";
    private static final String CUERPO = "{\"titulo\":\"Wifi caido\",\"prioridad\":\"ALTA\",\"solicitanteId\":3}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService ticketService;

    private RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private Ticket ticket() {
        Ticket t = new Ticket();
        t.setId(5L);
        t.setTitulo("Wifi caido");
        t.setPrioridad(Prioridad.ALTA);
        t.setSolicitanteId(3L);
        return t;
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
        when(ticketService.obtenerTodos()).thenReturn(List.of(ticket()));
        when(ticketService.obtenerPorId(anyLong())).thenReturn(ticket());
        when(ticketService.crear(any())).thenReturn(ticket());
        when(ticketService.asignar(anyLong(), anyLong())).thenReturn(ticket());
        when(ticketService.resolver(anyLong())).thenReturn(ticket());
        when(ticketService.reabrir(anyLong())).thenReturn(ticket());
        when(ticketService.actualizar(anyLong(), any())).thenReturn(ticket());
    }

    @Test
    void sinTokenNadaEsAccesibleSalvoLaSalud() throws Exception {
        mvc.perform(get("/tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/tickets/5")).andExpect(status().isUnauthorized());
        mvc.perform(post("/tickets").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/tickets/5/resolver")).andExpect(status().isUnauthorized());
        int salud = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();
        org.junit.jupiter.api.Assertions.assertNotEquals(401, salud);
    }

    @Test
    void cualquierRolPuedeVerYReabrirTickets() throws Exception {
        preparar();

        for (String rol : List.of("SOLICITANTE", "TECNICO", "ADMINISTRADOR")) {
            mvc.perform(get("/tickets").with(como(rol))).andExpect(status().isOk());
            mvc.perform(get("/tickets/5").with(como(rol))).andExpect(status().isOk());
            mvc.perform(put("/tickets/5/reabrir").with(como(rol))).andExpect(status().isOk());
        }
    }

    @Test
    void soloElSolicitanteYElAdministradorCreanTicketsElTecnicoNo() throws Exception {
        preparar();

        for (String rol : List.of("SOLICITANTE", "ADMINISTRADOR")) {
            mvc.perform(post("/tickets").with(como(rol)).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/tickets").with(como("TECNICO")).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
    }

    @Test
    void asignanElTecnicoYElAdministradorPeroNoElSolicitante() throws Exception {
        preparar();

        mvc.perform(put("/tickets/5/asignar/9").with(como("SOLICITANTE"))).andExpect(status().isForbidden());
        mvc.perform(put("/tickets/5/asignar/9").with(como("TECNICO"))).andExpect(status().isOk());
        mvc.perform(put("/tickets/5/asignar/9").with(como("ADMINISTRADOR"))).andExpect(status().isOk());
    }

    @Test
    void resuelvenElTecnicoYElAdministradorPeroNoElSolicitante() throws Exception {
        preparar();

        mvc.perform(put("/tickets/5/resolver").with(como("SOLICITANTE"))).andExpect(status().isForbidden());
        mvc.perform(put("/tickets/5/resolver").with(como("TECNICO"))).andExpect(status().isOk());
        mvc.perform(put("/tickets/5/resolver").with(como("ADMINISTRADOR"))).andExpect(status().isOk());
    }

    @Test
    void editarYEliminarSonSoloDelAdministrador() throws Exception {
        preparar();

        for (String rol : List.of("SOLICITANTE", "TECNICO")) {
            mvc.perform(put("/tickets/5").with(como(rol)).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                    .andExpect(status().isForbidden());
            mvc.perform(delete("/tickets/5").with(como(rol))).andExpect(status().isForbidden());
        }
        mvc.perform(put("/tickets/5").with(como("ADMINISTRADOR")).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk());
        mvc.perform(delete("/tickets/5").with(como("ADMINISTRADOR"))).andExpect(status().isNoContent());
    }

    @Test
    void unTokenRealFirmadoConLaClaveEsAceptadoYRespetaElRol() throws Exception {
        preparar();
        String solicitante = token("SOLICITANTE", SECRETO, "helpdesk-identidad", 3600);
        String admin = token("ADMINISTRADOR", SECRETO, "helpdesk-identidad", 3600);

        mvc.perform(get("/tickets").header("Authorization", "Bearer " + solicitante)).andExpect(status().isOk());
        mvc.perform(put("/tickets/5/asignar/9").header("Authorization", "Bearer " + solicitante))
                .andExpect(status().isForbidden());
        mvc.perform(put("/tickets/5/asignar/9").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
    }

    @Test
    void seRechazanTokensConOtraClaveVencidosDeOtroEmisorOMalFormados() throws Exception {
        String otraClave = token("ADMINISTRADOR", "otra-clave-distinta-de-al-menos-32-caracteres!", "helpdesk-identidad", 3600);
        String vencido = token("ADMINISTRADOR", SECRETO, "helpdesk-identidad", -3600);
        String otroEmisor = token("ADMINISTRADOR", SECRETO, "otro-emisor", 3600);

        for (String malo : List.of(otraClave, vencido, otroEmisor, "basura")) {
            mvc.perform(get("/tickets").header("Authorization", "Bearer " + malo)).andExpect(status().isUnauthorized());
        }
    }
}
