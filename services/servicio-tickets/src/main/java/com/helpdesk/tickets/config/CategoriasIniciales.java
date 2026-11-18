package com.helpdesk.tickets.config;

import com.helpdesk.tickets.model.Categoria;
import com.helpdesk.tickets.repository.CategoriaRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Si no hay ninguna categoria, crea unas basicas para que se puedan asignar a los tickets desde el primer arranque.
 * El administrador puede editarlas, agregar otras o eliminarlas (las que no tengan tickets).
 */
@Component
public class CategoriasIniciales implements ApplicationRunner {

    private static final Logger registro = LoggerFactory.getLogger(CategoriasIniciales.class);

    static final List<Categoria> BASICAS = List.of(
            new Categoria(null, "Hardware", "Equipos, perifericos y repuestos"),
            new Categoria(null, "Software", "Instalacion y fallas de programas"),
            new Categoria(null, "Red y conectividad", "Internet, wifi, VPN y cableado"),
            new Categoria(null, "Cuentas y accesos", "Usuarios, claves y permisos"),
            new Categoria(null, "Correo", "Correo electronico y calendario"),
            new Categoria(null, "Impresoras", "Impresion y escaneo"),
            new Categoria(null, "Otros", "Casos que no encajan en las demas"));

    private final CategoriaRepository categorias;

    public CategoriasIniciales(CategoriaRepository categorias) {
        this.categorias = categorias;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categorias.count() > 0) {
            return;
        }
        BASICAS.forEach(base -> categorias.save(new Categoria(null, base.getNombre(), base.getDescripcion())));
        registro.info("Categorias basicas creadas: {}", BASICAS.size());
    }
}
