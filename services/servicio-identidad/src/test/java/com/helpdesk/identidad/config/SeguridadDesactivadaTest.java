package com.helpdesk.identidad.config;

import com.helpdesk.identidad.controller.UsuarioController;
import com.helpdesk.identidad.service.UsuarioService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(SeguridadConfig.class)
class SeguridadDesactivadaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void conAuthEnabledApagadoTodoSigueAbiertoComoAntes() throws Exception {
        when(usuarioService.obtenerTodos()).thenReturn(List.of());

        mvc.perform(get("/usuarios")).andExpect(status().isOk());
        mvc.perform(delete("/usuarios/1")).andExpect(status().isNoContent());
    }
}
