package com.helpdesk.identidad.config;

import com.helpdesk.identidad.model.Rol;
import com.helpdesk.identidad.model.Usuario;
import com.helpdesk.identidad.repository.UsuarioRepository;
import com.helpdesk.identidad.service.UsuarioService;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Order(2)
public class AdminInicial implements ApplicationRunner {

    private static final Logger registro = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioService usuarios;
    private final UsuarioRepository repositorio;
    private final String username;
    private final String clave;
    private final String nombre;

    public AdminInicial(UsuarioService usuarios,
                        UsuarioRepository repositorio,
                        @Value("${admin.usuario:}") String username,
                        @Value("${admin.clave:}") String clave,
                        @Value("${admin.nombre:Administrador}") String nombre) {
        this.usuarios = usuarios;
        this.repositorio = repositorio;
        this.username = username;
        this.clave = clave;
        this.nombre = nombre;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || clave == null || clave.isBlank()) {
            return;
        }
        if (repositorio.findByUsername(username.trim().toLowerCase(Locale.ROOT)).isPresent()) {
            registro.info("El administrador inicial '{}' ya existe", username);
            return;
        }
        Usuario admin = new Usuario();
        admin.setNombre(nombre);
        admin.setRol(Rol.ADMINISTRADOR);
        admin.setUsername(username);
        admin.setClave(clave);
        try {
            usuarios.crear(admin);
            registro.info("Administrador inicial '{}' creado", username);
        } catch (ResponseStatusException e) {
            registro.warn("No se pudo crear el administrador inicial: {}", e.getReason());
        }
    }
}
