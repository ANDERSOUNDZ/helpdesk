package com.helpdesk.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Vive en el paquete raiz a proposito: @SpringBootApplication escanea hacia abajo,
// asi encuentra domain/, application/ e infrastructure/ sin configurar nada.
@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
