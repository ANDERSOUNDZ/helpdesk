package com.helpdesk.identidad.dto;

public record LoginResponse(String token, String tipo, long expiraEn, UsuarioSesion usuario) {
}
