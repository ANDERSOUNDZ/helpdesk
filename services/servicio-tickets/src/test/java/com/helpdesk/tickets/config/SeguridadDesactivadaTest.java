package com.helpdesk.tickets.config;

import com.helpdesk.tickets.controller.TicketController;
import com.helpdesk.tickets.service.TicketService;

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

@WebMvcTest(TicketController.class)
@Import(SeguridadConfig.class)
class SeguridadDesactivadaTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void conAuthEnabledApagadoTodoSigueAbiertoComoAntes() throws Exception {
        when(ticketService.obtenerTodos()).thenReturn(List.of());

        mvc.perform(get("/tickets")).andExpect(status().isOk());
        mvc.perform(delete("/tickets/5")).andExpect(status().isNoContent());
    }
}
