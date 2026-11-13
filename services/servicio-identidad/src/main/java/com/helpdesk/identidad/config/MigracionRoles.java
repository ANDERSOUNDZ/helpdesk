package com.helpdesk.identidad.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class MigracionRoles implements ApplicationRunner {

    private static final Logger registro = LoggerFactory.getLogger(MigracionRoles.class);

    private final JdbcTemplate jdbc;

    public MigracionRoles(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check");
            int cambiados = jdbc.update("UPDATE usuarios SET rol = 'SOLICITANTE' WHERE rol IN ('ESTUDIANTE', 'USUARIO')");
            jdbc.execute("ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check "
                    + "CHECK (rol IN ('SOLICITANTE', 'TECNICO', 'ADMINISTRADOR'))");
            if (cambiados > 0) {
                registro.info("Roles migrados a SOLICITANTE: {}", cambiados);
            }
        } catch (DataAccessException e) {
            registro.warn("No se pudo migrar la restriccion de roles: {}", e.getMostSpecificCause().getMessage());
        }
    }
}
