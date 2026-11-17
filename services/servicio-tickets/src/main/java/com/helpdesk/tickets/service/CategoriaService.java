package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.Categoria;
import com.helpdesk.tickets.repository.CategoriaRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CategoriaService {

    private static final int LARGO_MAX_NOMBRE = 100;
    private static final int LARGO_MAX_DESCRIPCION = 250;

    private final CategoriaRepository categorias;
    private final TicketRepository tickets;

    public CategoriaService(CategoriaRepository categorias, TicketRepository tickets) {
        this.categorias = categorias;
        this.tickets = tickets;
    }

    public List<Categoria> obtenerTodas() {
        return categorias.findAllByOrderByNombreAsc();
    }

    public Categoria obtenerPorId(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la categoria con id " + id));
    }

    public Categoria crear(Categoria datos) {
        String nombre = validarNombre(datos.getNombre());
        validarDescripcion(datos.getDescripcion());
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre");
        }
        Categoria nueva = new Categoria(null, nombre, datos.getDescripcion());
        return categorias.save(nueva);
    }

    public Categoria actualizar(Long id, Categoria datos) {
        Categoria categoria = obtenerPorId(id);
        if (datos.getNombre() != null) {
            String nombre = validarNombre(datos.getNombre());
            if (categorias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre");
            }
            categoria.setNombre(nombre);
        }
        if (datos.getDescripcion() != null) {
            validarDescripcion(datos.getDescripcion());
            categoria.setDescripcion(datos.getDescripcion());
        }
        return categorias.save(categoria);
    }

    public void eliminar(Long id) {
        Categoria categoria = obtenerPorId(id);
        if (tickets.existsByCategoriaId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la categoria porque tiene tickets asociados");
        }
        categorias.delete(categoria);
    }

    public void validarExiste(Long categoriaId) {
        if (categoriaId != null && !categorias.existsById(categoriaId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No existe la categoria con id " + categoriaId);
        }
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la categoria es obligatorio");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAX_NOMBRE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los " + LARGO_MAX_NOMBRE + " caracteres");
        }
        return limpio;
    }

    private void validarDescripcion(String descripcion) {
        if (descripcion != null && descripcion.length() > LARGO_MAX_DESCRIPCION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar los " + LARGO_MAX_DESCRIPCION + " caracteres");
        }
    }
}
