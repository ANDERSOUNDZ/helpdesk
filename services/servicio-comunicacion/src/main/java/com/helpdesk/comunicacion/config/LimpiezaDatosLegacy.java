package com.helpdesk.comunicacion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Elimina avisos de encuestas que quedaron en bases de datos anteriores.
 * La encuesta ya no forma parte del servicio y su valor de enum ya no existe.
 */
@Component
public class LimpiezaDatosLegacy implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDatosLegacy.class);

    private final JdbcTemplate jdbc;

    public LimpiezaDatosLegacy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int eliminadas = jdbc.update(
                    "DELETE FROM notificaciones WHERE tipo = 'ENCUESTA_SATISFACCION'");
            if (eliminadas > 0) {
                log.info("Se eliminaron {} avisos obsoletos de encuestas", eliminadas);
            }
        } catch (DataAccessException ex) {
            log.debug("No se pudieron limpiar avisos obsoletos de encuestas", ex);
        }
    }
}
