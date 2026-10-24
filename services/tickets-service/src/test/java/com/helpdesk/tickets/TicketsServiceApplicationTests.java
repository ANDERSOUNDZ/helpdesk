package com.helpdesk.tickets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Comprueba que el cableado completo (BeanConfig, controller, repositorio) arranca.
// Si alguien rompe una inyeccion de dependencias, este test lo detecta.
@SpringBootTest
class TicketsServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
