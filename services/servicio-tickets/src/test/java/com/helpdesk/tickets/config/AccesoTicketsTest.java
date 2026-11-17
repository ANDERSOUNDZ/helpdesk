package com.helpdesk.tickets.config;

import com.helpdesk.tickets.dto.TicketFiltros;
import com.helpdesk.tickets.model.Ticket;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccesoTicketsTest {

    private static final long SOLICITANTE = 10L;
    private static final long TECNICO_A = 1L;
    private static final long TECNICO_B = 2L;
    private static final long ADMIN = 8L;

    private final AccesoTickets acceso = new AccesoTickets();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private void iniciarSesion(long uid, String rol) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").claim("uid", uid)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }

    private Ticket ticket(Long solicitanteId, Long tecnicoId) {
        Ticket t = new Ticket();
        t.setId(5L);
        t.setSolicitanteId(solicitanteId);
        t.setTecnicoId(tecnicoId);
        return t;
    }

    private void esProhibido(Runnable accion) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(HttpStatus.FORBIDDEN.value(), e.getStatusCode().value());
    }

    @Test
    void sinSesionNoSeRestringeNada() {
        Ticket ajeno = ticket(99L, 77L);

        assertDoesNotThrow(() -> acceso.exigirVer(ajeno));
        assertDoesNotThrow(() -> acceso.exigirAsignar(ajeno));
        assertDoesNotThrow(() -> acceso.exigirResolver(ajeno));
        assertDoesNotThrow(() -> acceso.exigirReabrir(ajeno));
        assertEquals(3L, acceso.solicitanteParaCrear(3L));
        TicketFiltros filtros = new TicketFiltros(null, null, null, 4L, null, null, null);
        assertEquals(4L, acceso.restringir(filtros).solicitanteId());
    }

    @Test
    void elSolicitanteSoloVeYListaLoSuyoYCreaASuNombre() {
        iniciarSesion(SOLICITANTE, "SOLICITANTE");

        assertDoesNotThrow(() -> acceso.exigirVer(ticket(SOLICITANTE, null)));
        esProhibido(() -> acceso.exigirVer(ticket(11L, null)));

        TicketFiltros pedido = new TicketFiltros(null, null, null, 11L, null, null, null);
        assertEquals(SOLICITANTE, acceso.restringir(pedido).solicitanteId());
        assertEquals(SOLICITANTE, acceso.restringir(null).solicitanteId());
        assertEquals(SOLICITANTE, acceso.solicitanteParaCrear(11L));
    }

    @Test
    void elSolicitanteSoloReabreLoSuyo() {
        iniciarSesion(SOLICITANTE, "SOLICITANTE");

        assertDoesNotThrow(() -> acceso.exigirReabrir(ticket(SOLICITANTE, TECNICO_A)));
        esProhibido(() -> acceso.exigirReabrir(ticket(11L, TECNICO_A)));
    }

    @Test
    void elTecnicoVeTodoPeroNoReabre() {
        iniciarSesion(TECNICO_A, "TECNICO");

        assertDoesNotThrow(() -> acceso.exigirVer(ticket(99L, TECNICO_B)));
        assertNull(acceso.restringir(TicketFiltros.vacios()).solicitanteId());
        assertEquals(3L, acceso.solicitanteParaCrear(3L));
        esProhibido(() -> acceso.exigirReabrir(ticket(SOLICITANTE, TECNICO_A)));
    }

    @Test
    void unTicketTomadoSoloLoModificaQuienLoTiene() {
        iniciarSesion(TECNICO_A, "TECNICO");

        assertDoesNotThrow(() -> acceso.exigirAsignar(ticket(SOLICITANTE, null)));
        assertDoesNotThrow(() -> acceso.exigirAsignar(ticket(SOLICITANTE, TECNICO_A)));
        esProhibido(() -> acceso.exigirAsignar(ticket(SOLICITANTE, TECNICO_B)));

        assertDoesNotThrow(() -> acceso.exigirResolver(ticket(SOLICITANTE, TECNICO_A)));
        esProhibido(() -> acceso.exigirResolver(ticket(SOLICITANTE, TECNICO_B)));
        esProhibido(() -> acceso.exigirResolver(ticket(SOLICITANTE, null)));
    }

    @Test
    void elTecnicoSoloRegistraSusHorasEnSusTickets() {
        iniciarSesion(TECNICO_A, "TECNICO");

        assertDoesNotThrow(() -> acceso.exigirRegistrarHoras(ticket(SOLICITANTE, TECNICO_A), TECNICO_A));
        esProhibido(() -> acceso.exigirRegistrarHoras(ticket(SOLICITANTE, TECNICO_B), TECNICO_A));
        esProhibido(() -> acceso.exigirRegistrarHoras(ticket(SOLICITANTE, TECNICO_A), TECNICO_B));
    }

    @Test
    void elAdministradorPuedeTodo() {
        iniciarSesion(ADMIN, "ADMINISTRADOR");
        Ticket tomado = ticket(SOLICITANTE, TECNICO_B);

        assertDoesNotThrow(() -> acceso.exigirVer(tomado));
        assertDoesNotThrow(() -> acceso.exigirAsignar(tomado));
        assertDoesNotThrow(() -> acceso.exigirResolver(tomado));
        assertDoesNotThrow(() -> acceso.exigirReabrir(tomado));
        assertDoesNotThrow(() -> acceso.exigirRegistrarHoras(tomado, TECNICO_B));
        assertEquals(3L, acceso.solicitanteParaCrear(3L));
        assertNull(acceso.restringir(TicketFiltros.vacios()).solicitanteId());
    }
}
