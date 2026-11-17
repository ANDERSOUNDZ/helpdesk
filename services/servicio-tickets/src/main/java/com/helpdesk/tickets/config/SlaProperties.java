package com.helpdesk.tickets.config;

import com.helpdesk.tickets.model.Prioridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglas de SLA por prioridad. Se pueden cambiar por variables de entorno:
 * SLA_HORAS_ALTA, SLA_HORAS_MEDIA, SLA_HORAS_BAJA.
 */
@ConfigurationProperties(prefix = "sla")
public class SlaProperties {

    private long horasAlta = 8;
    private long horasMedia = 24;
    private long horasBaja = 72;
    private long anticipacionMinutos = 60;
    private boolean recordatoriosEnabled = true;

    public long horasPara(Prioridad prioridad) {
        if (prioridad == null) {
            return horasMedia;
        }
        return switch (prioridad) {
            case ALTA -> horasAlta;
            case MEDIA -> horasMedia;
            case BAJA -> horasBaja;
        };
    }

    public long getHorasAlta() { return horasAlta; }
    public void setHorasAlta(long horasAlta) { this.horasAlta = horasAlta; }

    public long getHorasMedia() { return horasMedia; }
    public void setHorasMedia(long horasMedia) { this.horasMedia = horasMedia; }

    public long getHorasBaja() { return horasBaja; }
    public void setHorasBaja(long horasBaja) { this.horasBaja = horasBaja; }

    public long getAnticipacionMinutos() { return anticipacionMinutos; }
    public void setAnticipacionMinutos(long anticipacionMinutos) { this.anticipacionMinutos = anticipacionMinutos; }

    public boolean isRecordatoriosEnabled() { return recordatoriosEnabled; }
    public void setRecordatoriosEnabled(boolean recordatoriosEnabled) { this.recordatoriosEnabled = recordatoriosEnabled; }
}
