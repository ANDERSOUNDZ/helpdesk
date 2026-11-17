package com.helpdesk.tickets.service;

import com.helpdesk.tickets.config.SlaProperties;
import com.helpdesk.tickets.evento.PublicadorEventos;
import com.helpdesk.tickets.evento.TicketSlaPorVencerEvent;
import com.helpdesk.tickets.evento.TicketSlaVencidoEvent;
import com.helpdesk.tickets.model.EstadoTicket;
import com.helpdesk.tickets.model.Ticket;
import com.helpdesk.tickets.repository.TicketRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Revisa periodicamente los tickets con SLA y publica un recordatorio.
 * Se puede apagar con SLA_RECORDATORIOS_ENABLED=false y ajustar el cron con
 * SLA_RECORDATORIOS_CRON.
 */
@Service
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private final TicketRepository tickets;
    private final PublicadorEventos eventos;
    private final SlaProperties propiedades;

    public SlaService(TicketRepository tickets, PublicadorEventos eventos, SlaProperties propiedades) {
        this.tickets = tickets;
        this.eventos = eventos;
        this.propiedades = propiedades;
    }

    @Scheduled(cron = "${sla.recordatorios-cron:0 */15 * * * *}")
    public void ejecutarProgramado() {
        if (!propiedades.isRecordatoriosEnabled()) {
            return;
        }
        int enviados = revisar(Instant.now());
        if (enviados > 0) {
            log.info("Recordatorios de SLA publicados: {}", enviados);
        }
    }

    /** Separado del @Scheduled para poder probarlo sin esperar al cron. */
    public int revisar(Instant ahora) {
        Instant limite = ahora.plus(propiedades.getAnticipacionMinutos(), ChronoUnit.MINUTES);
        List<Ticket> pendientes = tickets
                .findByEstadoNotAndFechaLimiteBeforeAndRecordatorioEnviadoEnIsNull(
                        EstadoTicket.RESUELTO, limite);

        for (Ticket ticket : pendientes) {
            if (ticket.getFechaLimite().isAfter(ahora)) {
                eventos.publicar(new TicketSlaPorVencerEvent(
                        ticket.getId(),
                        ticket.getTitulo(),
                        ticket.getPrioridad() == null ? null : ticket.getPrioridad().name(),
                        ticket.getFechaLimite(),
                        ticket.getSolicitanteId(),
                        ticket.getTecnicoId(),
                        ahora));
            } else {
                eventos.publicar(new TicketSlaVencidoEvent(
                        ticket.getId(),
                        ticket.getTitulo(),
                        ticket.getPrioridad() == null ? null : ticket.getPrioridad().name(),
                        ticket.getFechaLimite(),
                        ticket.getSolicitanteId(),
                        ticket.getTecnicoId(),
                        ahora));
            }
            ticket.setRecordatorioEnviadoEn(ahora);
        }

        if (!pendientes.isEmpty()) {
            tickets.saveAll(pendientes);
        }
        return pendientes.size();
    }
}
