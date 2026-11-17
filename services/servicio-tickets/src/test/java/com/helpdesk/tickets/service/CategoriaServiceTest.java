package com.helpdesk.tickets.service;

import com.helpdesk.tickets.model.Categoria;
import com.helpdesk.tickets.repository.CategoriaRepository;
import com.helpdesk.tickets.repository.TicketRepository;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoriaServiceTest {

    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final CategoriaService service = new CategoriaService(categorias, tickets);

    @BeforeEach
    void preparar() {
        when(categorias.save(any(Categoria.class))).thenAnswer(llamada -> {
            Categoria categoria = llamada.getArgument(0);
            if (categoria.getId() == null) {
                categoria.setId(1L);
            }
            return categoria;
        });
    }

    @Test
    void creaUnaCategoriaNormalizandoElNombre() {
        Categoria creada = service.crear(new Categoria(null, "  Redes  ", "Problemas de red"));

        assertEquals("Redes", creada.getNombre());
        assertEquals(1L, creada.getId());
    }

    @Test
    void rechazaNombreVacioYDuplicado() {
        assertThrows(ResponseStatusException.class,
                () -> service.crear(new Categoria(null, "  ", null)));

        when(categorias.existsByNombreIgnoreCase("Redes")).thenReturn(true);
        assertThrows(ResponseStatusException.class,
                () -> service.crear(new Categoria(null, "Redes", null)));
    }

    @Test
    void noEliminaUnaCategoriaConTickets() {
        Categoria categoria = new Categoria(1L, "Redes", null);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(tickets.existsByCategoriaId(1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.eliminar(1L));
    }

    @Test
    void eliminaUnaCategoriaSinTickets() {
        Categoria categoria = new Categoria(1L, "Redes", null);
        when(categorias.findById(1L)).thenReturn(Optional.of(categoria));
        when(tickets.existsByCategoriaId(1L)).thenReturn(false);

        service.eliminar(1L);

        verify(categorias).delete(categoria);
    }
}
