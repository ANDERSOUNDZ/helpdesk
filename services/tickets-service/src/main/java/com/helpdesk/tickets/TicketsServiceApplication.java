package com.helpdesk.tickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Esta clase vive en el paquete raiz a proposito: @SpringBootApplication escanea
// hacia abajo, asi que encuentra api/, infrastructure/ y application/ sin configurar nada.
@SpringBootApplication
public class TicketsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketsServiceApplication.class, args);
    }
}
