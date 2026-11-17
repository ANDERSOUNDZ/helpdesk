package com.helpdesk.tickets.evento;

/**
 * Contrato interno de salida de eventos. El servicio de tickets solo depende de esta interfaz:
 * cambiar de log a RabbitMQ (o a otro broker) no toca la logica de negocio.
 */
public interface PublicadorEventos {

    void publicar(TicketCreadoEvent evento);

    void publicar(TicketAsignadoEvent evento);

    void publicar(TicketEstadoCambiadoEvent evento);

    void publicar(TicketSlaPorVencerEvent evento);

    void publicar(TicketSlaVencidoEvent evento);
}
