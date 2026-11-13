package com.helpdesk.identidad.controller;

import com.helpdesk.identidad.dto.LoginRequest;
import com.helpdesk.identidad.dto.LoginResponse;
import com.helpdesk.identidad.service.AuthService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@RequestBody LoginRequest pedido) {
        return authService.login(pedido);
    }
}
