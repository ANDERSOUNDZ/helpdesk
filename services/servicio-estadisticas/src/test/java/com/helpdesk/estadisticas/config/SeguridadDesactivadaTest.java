package com.helpdesk.estadisticas.config;

import com.helpdesk.estadisticas.controller.EstadisticasController;
import com.helpdesk.estadisticas.model.ResumenTickets;
import com.helpdesk.estadisticas.service.EstadisticasService;

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

@WebMvcTest(EstadisticasController.class)
@Import(SeguridadConfig.class)
class SeguridadDesactivadaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EstadisticasService estadisticasService;

    @Test
    void conAuthEnabledApagadoTodoSigueAbiertoComoAntes() throws Exception {
        when(estadisticasService.obtenerResumen()).thenReturn(new ResumenTickets(0, 0, 0));

        mvc.perform(get("/resumen")).andExpect(status().isOk());
        mvc.perform(delete("/estadisticas/reset")).andExpect(status().isNoContent());
    }
}
