package com.helpdesk.comunicacion.config;

import com.helpdesk.comunicacion.controller.ComunicacionController;
import com.helpdesk.comunicacion.controller.ManejadorErrores;
import com.helpdesk.comunicacion.service.AdjuntoService;
import com.helpdesk.comunicacion.service.ComunicacionService;
import com.helpdesk.comunicacion.service.EncuestaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComunicacionController.class)
@Import({SeguridadConfig.class, ManejadorErrores.class})
class SeguridadDesactivadaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ComunicacionService comunicacionService;

    @MockitoBean
    private EncuestaService encuestaService;

    @MockitoBean
    private AdjuntoService adjuntoService;

    @Test
    void conAuthEnabledApagadoTodoSigueAbiertoComoAntes() throws Exception {
        mvc.perform(get("/comentarios")).andExpect(status().isOk());
        mvc.perform(delete("/comentarios/1")).andExpect(status().isNoContent());
    }
}
