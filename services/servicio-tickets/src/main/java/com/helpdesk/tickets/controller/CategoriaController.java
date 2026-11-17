package com.helpdesk.tickets.controller;

import com.helpdesk.tickets.model.Categoria;
import com.helpdesk.tickets.service.CategoriaService;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class CategoriaController {

    private final CategoriaService categorias;

    public CategoriaController(CategoriaService categorias) {
        this.categorias = categorias;
    }

    @GetMapping("/categorias")
    public List<Categoria> obtenerCategorias() {
        return categorias.obtenerTodas();
    }

    @GetMapping("/categorias/{id}")
    public Categoria obtenerCategoria(@PathVariable Long id) {
        return categorias.obtenerPorId(id);
    }

    @PostMapping("/categorias")
    public ResponseEntity<Categoria> crearCategoria(@Valid @RequestBody Categoria datos) {
        Categoria creada = categorias.crear(datos);
        return ResponseEntity.created(URI.create("/categorias/" + creada.getId())).body(creada);
    }

    @PutMapping("/categorias/{id}")
    public Categoria actualizarCategoria(@PathVariable Long id, @Valid @RequestBody Categoria datos) {
        return categorias.actualizar(id, datos);
    }

    @DeleteMapping("/categorias/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        categorias.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
