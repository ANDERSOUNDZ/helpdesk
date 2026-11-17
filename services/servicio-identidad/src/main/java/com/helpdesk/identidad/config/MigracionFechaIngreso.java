package com.helpdesk.identidad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Los usuarios creados antes de existir la columna fecha_ingreso la reciben vacia. Se rellena una sola vez con la
 * fecha del arranque (es una fecha aproximada: no hay forma de saber la real).
 */
@Component
@Order(1)
public class MigracionFechaIngreso implements ApplicationRunner {

    private static final Logger registro = LoggerFactory.getLogger(MigracionFechaIngreso.class);

    private final JdbcTemplate jdbc;

    public MigracionFechaIngreso(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int rellenados = jdbc.update("UPDATE usuarios SET fecha_ingreso = NOW() WHERE fecha_ingreso IS NULL");
            if (rellenados > 0) {
                registro.info("Usuarios sin fecha de ingreso rellenados con la fecha actual: {}", rellenados);
            }
        } catch (DataAccessException e) {
            registro.warn("No se pudo rellenar la fecha de ingreso: {}", e.getMostSpecificCause().getMessage());
        }
    }
}
