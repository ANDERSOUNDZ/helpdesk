package com.helpdesk.tickets.config;

import com.helpdesk.tickets.model.Categoria;
import com.helpdesk.tickets.repository.CategoriaRepository;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoriasInicialesTest {

    private final CategoriaRepository repositorio = mock(CategoriaRepository.class);
    private final CategoriasIniciales inicial = new CategoriasIniciales(repositorio);

    @Test
    void creaLasBasicasCuandoNoHayNinguna() {
        when(repositorio.count()).thenReturn(0L);

        inicial.run(null);

        verify(repositorio, times(CategoriasIniciales.BASICAS.size())).save(any(Categoria.class));
    }

    @Test
    void noTocaNadaSiYaHayCategorias() {
        when(repositorio.count()).thenReturn(3L);

        inicial.run(null);

        verify(repositorio, never()).save(any(Categoria.class));
    }
}
