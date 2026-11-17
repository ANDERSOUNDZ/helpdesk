package com.helpdesk.tickets.config;

import com.helpdesk.tickets.dto.TicketFiltros;
import com.helpdesk.tickets.model.Ticket;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Reglas de "quien ve y quien toca cada ticket" segun el usuario del token:
 * <ul>
 *   <li>SOLICITANTE: solo ve y reabre sus propios tickets; los crea siempre a su nombre.</li>
 *   <li>TECNICO: ve todos. Puede tomar un ticket sin tecnico; una vez tomado solo lo modifica quien lo tiene.</li>
 *   <li>ADMINISTRADOR: ve y modifica todo.</li>
 * </ul>
 * Con el login apagado (sin token) no se restringe nada.
 */
@Component
public class AccesoTickets {

    public record Actor(Long id, String rol) {
        public boolean esSolicitante() { return "SOLICITANTE".equals(rol); }
        public boolean esTecnico() { return "TECNICO".equals(rol); }
        public boolean esAdministrador() { return "ADMINISTRADOR".equals(rol); }
    }

    /** Usuario autenticado; vacio si no hay token (login apagado). */
    public Optional<Actor> actual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !(autenticacion.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }
        Object uid = jwt.getClaim("uid");
        Long id = uid instanceof Number numero ? numero.longValue() : null;
        String rol = autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
        return Optional.of(new Actor(id, rol));
    }

    /** El solicitante solo lista lo suyo, sin importar el filtro que mande. */
    public TicketFiltros restringir(TicketFiltros filtros) {
        TicketFiltros base = filtros == null ? TicketFiltros.vacios() : filtros;
        return actual().filter(Actor::esSolicitante)
                .map(actor -> new TicketFiltros(base.estado(), base.prioridad(), base.tecnicoId(),
                        actor.id(), base.categoriaId(), base.q(), base.vencidos()))
                .orElse(base);
    }

    /** Un solicitante solo ve sus tickets. */
    public void exigirVer(Ticket ticket) {
        actual().filter(Actor::esSolicitante).ifPresent(actor -> {
            if (!actor.id().equals(ticket.getSolicitanteId())) {
                throw prohibido("No tienes acceso a este ticket");
            }
        });
    }

    /** Los solicitantes siempre crean a su nombre; el administrador puede crear por otra persona. */
    public Long solicitanteParaCrear(Long solicitanteIdPedido) {
        return actual().filter(Actor::esSolicitante).map(Actor::id).orElse(solicitanteIdPedido);
    }

    /** Un tecnico solo toma tickets sin tecnico (o los suyos); el administrador reasigna cuando quiera. */
    public void exigirAsignar(Ticket ticket) {
        actual().filter(Actor::esTecnico).ifPresent(actor -> {
            Long actual = ticket.getTecnicoId();
            if (actual != null && !actual.equals(actor.id())) {
                throw prohibido("El ticket ya fue tomado por otro tecnico: solo puedes verlo");
            }
        });
    }

    /** Solo el tecnico que tiene el ticket (o el administrador) puede finalizarlo. */
    public void exigirResolver(Ticket ticket) {
        actual().filter(Actor::esTecnico).ifPresent(actor -> {
            if (!actor.id().equals(ticket.getTecnicoId())) {
                throw prohibido("Solo el tecnico asignado puede finalizar este ticket");
            }
        });
    }

    /** Reabre el solicitante dueno del ticket o el administrador; el tecnico no. */
    public void exigirReabrir(Ticket ticket) {
        actual().ifPresent(actor -> {
            if (actor.esTecnico()) {
                throw prohibido("Solo el solicitante o el administrador pueden reabrir un ticket");
            }
            if (actor.esSolicitante() && !actor.id().equals(ticket.getSolicitanteId())) {
                throw prohibido("Solo puedes reabrir tus propios tickets");
            }
        });
    }

    /** El tecnico solo registra horas propias y en tickets que tiene. */
    public void exigirRegistrarHoras(Ticket ticket, Long tecnicoDeLasHoras) {
        actual().filter(Actor::esTecnico).ifPresent(actor -> {
            if (!actor.id().equals(ticket.getTecnicoId())) {
                throw prohibido("Solo el tecnico asignado puede registrar horas en este ticket");
            }
            if (!actor.id().equals(tecnicoDeLasHoras)) {
                throw prohibido("Solo puedes registrar tus propias horas");
            }
        });
    }

    private ResponseStatusException prohibido(String mensaje) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, mensaje);
    }
}
